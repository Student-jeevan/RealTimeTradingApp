# TradingApp — System Behavior & Testing Documentation

> **Purpose**: This document describes exactly how every module works, where Redis is used, how Kafka events flow through the system, and what to test at unit and integration levels.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Infrastructure (Docker Compose)](#2-infrastructure-docker-compose)
3. [Security & Authentication Module](#3-security--authentication-module)
4. [Redis — All Usage Patterns](#4-redis--all-usage-patterns)
5. [Kafka — Events, Producers & Consumers](#5-kafka--events-producers--consumers)
6. [Order & Trade Execution Module](#6-order--trade-execution-module)
7. [Wallet & Ledger Module](#7-wallet--ledger-module)
8. [Coin / Market Data Module](#8-coin--market-data-module)
9. [Withdrawal Module](#9-withdrawal-module)
10. [Payment Module](#10-payment-module)
11. [Notification Module](#11-notification-module)
12. [WebSocket Real-Time Pipeline](#12-websocket-real-time-pipeline)
13. [Analytics Module](#13-analytics-module)
14. [Unit Testing Guide — What to Test Per Module](#14-unit-testing-guide)
15. [Integration Testing Guide](#15-integration-testing-guide)

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend (React + Redux)              │
│            HTTP REST + WebSocket (STOMP)                │
└──────────────────────────┬──────────────────────────────┘
                           │ JWT in Authorization header
┌──────────────────────────▼──────────────────────────────┐
│           Spring Boot Backend  :8081                    │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Controllers  │  │   Services   │  │  Repositories│  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                  │          │
│  ┌──────▼─────────────────▼──────┐    MySQL  │          │
│  │    Kafka Producers            │◄──────────┘          │
│  └──────┬────────────────────────┘                      │
└─────────┼───────────────────────────────────────────────┘
          │
┌─────────▼──────────────────────────────────────────────┐
│                Apache Kafka (Confluent)                 │
│  Topics: trade-events | price-updates |                 │
│          notification-events | transaction-events       │
└─────────┬──────────────────────────────────────────────┘
          │
┌─────────▼─────────────────────────────────────────────┐
│                Kafka Consumers                         │
│  TradeEventConsumer → NotificationProducer + TxnAudit │
│  PriceUpdateConsumer → Redis cache + WebSocket push   │
│  NotificationConsumer → Email + WebSocket             │
│  TransactionConsumer → audit_log table (JDBC)         │
└────────────────────────────────────────────────────────┘

          External price feed
┌─────────────────────────────────────────────────────────┐
│  BinanceWebSocketClient                                 │
│  Subscribes to wss://stream.binance.com !miniTicker@arr │
│  → PriceUpdateProducer → Kafka                          │
└─────────────────────────────────────────────────────────┘

          Cache layer
┌────────────────────────┐
│   Redis (port 6379)    │
│  market:price:{coinId} │  ← hot price cache (5 min TTL)
│  market:chart:{id}:{d} │  ← chart data (5 min TTL)
│  market:details:{id}   │  ← coin details (5 min TTL)
│  market:top50          │  ← top 50 (15 min TTL)
│  market:trending       │  ← trending (15 min TTL)
│  otp:{email}           │  ← OTP value (5 min TTL)
│  otp_attempts:{email}  │  ← fail counter (5 min TTL)
│  otp_cooldown:{email}  │  ← resend guard (30 sec TTL)
└────────────────────────┘
```

---

## 2. Infrastructure (Docker Compose)

| Service | Image | Port | Role |
|---|---|---|---|
| `zookeeper` | cp-zookeeper:7.5.0 | 2181 | Kafka coordination |
| `kafka` | cp-kafka:7.5.0 | 9092 (internal), 29092 (host) | Message broker |
| `kafka-init` | cp-kafka:7.5.0 | — | Creates topics on startup |
| `redis` | redis:7-alpine | 6379 | Caching + OTP storage |
| `backend` | custom Spring Boot | 8081 | REST API + consumers |
| `frontend` | custom Vite/React | 3001→5173 | SPA |

**Topic Creation** (via `kafka-init` container):
```
trade-events         → 3 partitions, RF=1
price-updates        → 3 partitions, RF=1
notification-events  → 3 partitions, RF=1
transaction-events   → 3 partitions, RF=1
analytics-events     → 3 partitions, RF=1
```
Topics are also declared as `@Bean NewTopic` Spring beans (idempotent — safe to run multiple times).

---

## 3. Security & Authentication Module

### 3.1 JWT Authentication

**Classes:**
- `JwtProvider` — generates and parses JWT tokens
- `JwtTokenValidator` — Spring Security `OncePerRequestFilter`
- `JwtConstant` — holds the HMAC secret key string
- `AppConfig` — `SecurityFilterChain` configuration

**Token behavior:**
- Generated using JJWT with HMAC-SHA signing key
- Expires after **30 minutes** (`1800000 ms`)
- Claims embedded: `email`, `authorities` (comma-separated roles)
- All `/api/**`, `/auth/**`, `/ws/**` paths are open (`.permitAll()`)
- JWT filter is inserted `BEFORE BasicAuthenticationFilter`

**Token Validation flow:**
```
Request → JwtTokenValidator.doFilterInternal()
  → extracts "Bearer {token}" from Authorization header
  → parses claims (email, authorities)
  → creates UsernamePasswordAuthenticationToken
  → sets in SecurityContextHolder
  → continues filter chain
```

**CORS:**
- All origins allowed (`*`)
- Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
- Exposed header: `Authorization`
- Credentials: allowed; Max-Age: 3600s

### 3.2 OTP (Two-Factor Auth)

Stored entirely in **Redis**. See Section 4.1 for detailed Redis keys.

**Flow:**
1. User hits `/auth/send-otp`
2. `OtpService.generateAndStoreOtp(email)` → checks cooldown → generates 6-digit OTP → stores in Redis with 5 min TTL → sets 30s cooldown key
3. User submits OTP
4. `OtpService.verifyOtp(email, otp)` → checks attempt counter → validates → clears all keys on success

---

## 4. Redis — All Usage Patterns

### 4.1 OTP Service (`OtpService.java`)

| Redis Key | Type | TTL | Purpose |
|---|---|---|---|
| `otp:{email}` | String | 5 min | Stores the 6-digit OTP value |
| `otp_attempts:{email}` | String (int) | 5 min | Tracks wrong attempts (max 3) |
| `otp_cooldown:{email}` | String | 30 sec | Prevents resend spamming |

**Behavior:**
- Max 3 attempts; on 3rd failure → all OTP keys cleared, must request new OTP
- `clearOtp(email)` deletes all 3 keys atomically
- `canResendOtp(email)` → checks if cooldown key exists

### 4.2 Market Price Hot Cache (`MarketDataCacheService.java`)

| Redis Key | Type | TTL | Purpose |
|---|---|---|---|
| `market:price:{coinId}` | Hash | 5 min | Real-time price data per coin |

**Hash fields stored:**
```
coinId, symbol, name, currentPrice, priceChange24h,
priceChangePercentage24h, marketCap, totalVolume, timestamp
```

**Write path:**  
`PriceUpdateConsumer.consume()` → `cacheService.setPrice(coinId, priceData)` → `redisTemplate.opsForHash().putAll(key, data)` + `expire(key, 5min)`

**Read path:**  
`CoinServiceImpl.getCoinList()` → `cacheService.getAllPrices()` → pattern scan `market:price:*` → returns all cached coins (sub-second freshness)

### 4.3 CoinGecko API Response Cache (`CoinServiceImpl.java`)

| Redis Key | Type | TTL | Purpose |
|---|---|---|---|
| `market:chart:{coinId}:{days}` | String (JSON) | 5 min | Historical chart data |
| `market:details:{coinId}` | String (JSON) | 5 min | Full coin metadata |
| `market:top50` | String (JSON) | 15 min | Top 50 market cap coins |
| `market:trending` | String (JSON) | 15 min | Currently trending coins |

**Pattern**: Check cache → if miss → call CoinGecko API → store in Redis → return to caller.

### 4.4 Rate Limiter Fallback

`CoinServiceImpl` uses `@RateLimiter(name = "coingecko")` (Resilience4j). If rate-limited:
- Fallback methods (`getMarketChartFallback`, `getCoinDetailsFallback`, etc.) serve stale data from Redis cache
- If Redis also empty → throws `CustomException("Rate limit exceeded and no cached data available")`

### 4.5 Redis Configuration (`RedisConfig.java`)

```
Host: redis (Docker service name)
Port: 6379
Client: Lettuce (reactive, non-blocking)
Key serializer: StringRedisSerializer
Value serializer: GenericJackson2JsonRedisSerializer (JSON)
Default CacheManager TTL: 10 minutes
```

> **IMPORTANT for testing**: When writing integration tests, use `@EmbeddedRedis` or Testcontainers Redis, and configure the host to `localhost`.

---

## 5. Kafka — Events, Producers & Consumers

### 5.1 Kafka Configuration (`KafkaConfig.java`)

**Producer settings:**
- `acks=all` — waits for all replica acknowledgements (durability)
- `enable.idempotence=true` — exactly-once semantics, no duplicates
- Key serializer: `StringSerializer`
- Value serializer: `JsonSerializer` (Spring Kafka)

**Consumer settings:**
- Key deserializer: `StringDeserializer`
- Value deserializer: `JsonDeserializer`
- Trusted packages: `com.jeevan.TradingApp.kafka.events`
- `auto.offset.reset=earliest` — replay from beginning on new consumer group
- Concurrency: **3** (matches partition count)

---

### 5.2 Topic: `trade-events`

**Schema: `TradeEvent`**
```java
{
  eventId: String (UUID),
  timestamp: LocalDateTime,
  userId: Long,
  userEmail: String,
  orderId: Long,
  orderType: String,      // "BUY" or "SELL"
  coinId: String,
  coinSymbol: String,
  quantity: double,
  price: BigDecimal,
  status: String          // "FILLED"
}
```

**Producer: `TradeEventProducer`**
- Partition key: `orderId.toString()` → all events for the same order go to the same partition
- Called from: `OrderServiceImpl.publishTradeEvent()` → after both `buyAsset()` and `sellAsset()` complete successfully
- Fire-and-forget with `CompletableFuture.whenComplete` logging

**Consumer: `TradeEventConsumer`** — Group: `trade-group`
1. **Idempotency check**: queries `ProcessedEvent` table by `eventId`. If found → skip (avoids duplicate side-effects on consumer restart)
2. **Publishes** `NotificationEvent` to `notification-events` topic (trade confirmation email)
3. **Publishes** `TransactionEvent` to `transaction-events` topic (audit log)
4. **Saves** `ProcessedEvent(eventId)` to mark as done
5. On exception → re-throws (triggers Kafka retry / DLT)

---

### 5.3 Topic: `price-updates`

**Schema: `PriceUpdateEvent`**
```java
{
  eventId: String (UUID),
  timestamp: LocalDateTime,
  coinId: String,
  coinSymbol: String,
  coinName: String,
  currentPrice: double,
  priceChange24h: double,
  priceChangePercentage24h: double,
  marketCap: long,
  totalVolume: long
}
```

**Source: `BinanceWebSocketClient`**
- Connects to `wss://stream.binance.com:9443/ws/!miniTicker@arr`
- Receives all-symbol ticker updates every ~1 second
- Filters to a configured set of `trackedSymbols` (e.g., `btcusdt`, `ethusdt`)
- Maps Binance symbol (e.g., `btcusdt`) → coinId (`btc`) by stripping `usdt` suffix
- Publishes `PriceUpdateEvent` via `PriceUpdateProducer`
- **Exponential backoff reconnection**: delays 1s, 2s, 4s, ... up to 60s on disconnect

**Producer: `PriceUpdateProducer`**
- Partition key: `coinId` → all price updates for the same coin go to the same partition (strict ordering)
- DEBUG level logging (high frequency)

**Consumer: `PriceUpdateConsumer`** — Group: `price-group`
1. **Updates Redis** hot cache: `cacheService.setPrice(coinId, priceData)` with 5-minute TTL
2. **Pushes to WebSocket**: per-coin topic `/topic/prices/{coinId}` AND global `/topic/prices`
3. **Checks price alerts**: queries `PriceAlertRepository.findByCoinAndTriggeredFalse(coinId)` → for each active alert evaluates `ABOVE` / `BELOW` condition → triggers `PriceAlertService.triggerAlert()` if met
4. Errors are caught and logged (non-fatal — does NOT re-throw, so no retry)

---

### 5.4 Topic: `notification-events`

**Schema: `NotificationEvent`**
```java
{
  eventId: String (UUID),
  timestamp: LocalDateTime,
  userId: Long,
  email: String,
  type: String,    // "TRADE", "ALERT", "WITHDRAWAL_APPROVED", "WITHDRAWAL_REJECTED"
  subject: String,
  body: String     // HTML email body
}
```

**Producers that publish to this topic:**
- `TradeEventConsumer` → trade confirmation
- `WithdrawalServiceImpl.publishWithdrawalNotification()` → approval/rejection
- `PriceAlertServiceImpl.triggerAlert()` → price alert hit

**Consumer: `NotificationConsumer`** — Group: `notification-group`
1. **Idempotency check** via `ProcessedEventRepository`
2. **Sends HTML email** via `JavaMailSender` (MimeMessage with HTML body)
3. **Pushes WebSocket notification** to user: `messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload)`
4. **Saves** `ProcessedEvent(eventId)`
5. On exception → re-throws `RuntimeException` (triggers retry / DLT named `notification-events.DLT`)

---

### 5.5 Topic: `transaction-events`

**Schema: `TransactionEvent`**
```java
{
  eventId: String (UUID),
  timestamp: LocalDateTime,
  userId: Long,
  transactionType: String,  // "DEBIT" or "CREDIT"
  amount: BigDecimal,
  referenceId: String,      // orderId
  description: String
}
```

**Producer: `TransactionEventProducer`**
- Partition key: `userId.toString()`
- Called from: `TradeEventConsumer` after processing a `TradeEvent`

**Consumer: `TransactionConsumer`** — Group: `txn-audit-group`
1. **Idempotency check** via `ProcessedEventRepository`
2. **JDBC insert** into `audit_log` table (immutable audit record):
   ```sql
   INSERT INTO audit_log (event_id, user_id, transaction_type, amount, reference_id, description, event_timestamp)
   VALUES (?, ?, ?, ?, ?, ?, ?)
   ```
3. **Saves** `ProcessedEvent(eventId)`
4. On exception → re-throws (triggers retry / DLT)

---

### 5.6 Kafka Error Handling (`KafkaErrorHandler.java`)

- Dead Letter Topic (DLT) pattern: failed messages after 3 retries go to `{topic}.DLT`
- Logged with full stack trace

---

### 5.7 Idempotency Pattern (All Consumers)

All consumers use the `processed_events` table:
```
Table: processed_events
Column: event_id (String, unique)
```
Before processing any event: `processedEventRepository.existsByEventId(eventId)` → if true → skip.
After successful processing: `processedEventRepository.save(new ProcessedEvent(eventId))`.

This ensures **at-least-once** delivery with **exactly-once** side effects.

---

## 6. Order & Trade Execution Module

### 6.1 BUY Order Flow (`OrderServiceImpl.buyAsset`)

```
User sends POST /api/order/pay?orderType=BUY&coinId=bitcoin&quantity=0.5
      │
      ▼
1. Quantity validation (> 0)
2. RiskValidationService.validateTrade(user, request, price)
   └─ checks max order size, min balance thresholds
3. createOrderItem(coin, qty, buyPrice, 0) → save to DB
4. createOrder(user, orderItem, BUY) → Status: CREATED
5. status → VALIDATED
6. walletService.payOrderPayment(order, user)
   └─ checks availableBalance >= orderPrice
   └─ creates TRADE_LOCK ledger entry (locks funds)
   └─ status → OPEN
7. Immediate fill simulation:
   filledQty = qty, remainingQty = 0, status = FILLED
8. feeService.deductFee(user, price, orderId, "Fee for BUY order")
9. walletService.releaseLock(user, price, orderId, "Release lock")
10. walletService.debit(user, price, orderId, "Execute BUY order")
11. orderRepository.save(order)
12. assetService.createAsset OR updateAsset (add qty to portfolio)
13. tradeEventProducer.publish(TradeEvent) → Kafka "trade-events"
```

### 6.2 SELL Order Flow (`OrderServiceImpl.sellAsset`)

```
1. Quantity validation (> 0)
2. assetService.findAssetByUserIdAndCoinId — must exist
3. assetToSell.quantity >= sellQty (InsufficientAssetException otherwise)
4. RiskValidationService.validateTrade(user, request, sellPrice)
5. createOrderItem, createOrder → VALIDATED → OPEN → FILLED
6. walletService.credit(user, price, orderId, "Execute SELL order")
7. feeService.deductFee(user, price, orderId, "Fee for SELL order")
8. orderRepository.save
9. assetService.updateAsset(id, -qty) — subtract quantity
10. If remaining value < $1 → assetService.deleteAsset (dust cleanup)
11. tradeEventProducer.publish(TradeEvent) → Kafka "trade-events"
```

### 6.3 Order Status Lifecycle

```
CREATED → VALIDATED → OPEN → FILLED
                           → CANCELLED
                           → REJECTED
```

### 6.4 Order Cancellation

- Only the order owner can cancel
- Cannot cancel if status is FILLED, CANCELLED, or REJECTED
- If BUY + OPEN: releases locked funds proportional to remaining quantity

---

## 7. Wallet & Ledger Module

### 7.1 Wallet Operations

| Method | Ledger Entry Type | Effect on Balance |
|---|---|---|
| `addBalance()` | `CREDIT` | balance += amount |
| `debit()` | `DEBIT` | balance -= amount |
| `credit()` | `CREDIT` | balance += amount |
| `payOrderPayment()` | `TRADE_LOCK` | lockedBalance += amount |
| `releaseLock()` | `TRADE_RELEASE` | lockedBalance -= amount |
| `walletToWalletTransfer()` | `DEBIT` sender + `CREDIT` receiver | transfer |

### 7.2 Available Balance

```
availableBalance = balance - lockedBalance
```
Trade locks prevent double-spending during order execution.

### 7.3 Ledger Design

Every balance change is recorded as an immutable `WalletLedger` entry:
- `userId`, `transactionType`, `amount`, `referenceId`, `description`, `timestamp`
- `calculateAvailableBalance(userId)` recomputes from ledger history

---

## 8. Coin / Market Data Module

### 8.1 `getCoinList(page, size)`

```
1. cacheService.getAllPrices()
   → scan Redis keys "market:price:*"
   → if found: merge with DB Coin entities for full metadata
   → sort by marketCapRank, paginate, return
2. Fallback (Redis empty): fetchCoinListFromCoinGecko()
   → call CoinGecko /coins/markets?vs_currency=usd
   → parse JSON → return PageImpl
```

### 8.2 `getMarketChart(coinId, days)`

```
1. Check Redis "market:chart:{coinId}:{days}"
2. On cache hit → return immediately
3. On cache miss → call CoinGecko /coins/{id}/market_chart
4. Store result in Redis (5 min TTL)
5. @RateLimiter(name="coingecko") → if limited → fallback → serve stale or throw
```

### 8.3 Similar pattern for `getCoinDetails`, `getTop50`, `getTrendingCoins`

---

## 9. Withdrawal Module

### 9.1 Request Withdrawal (User)

```
POST /api/withdrawal/request?amount=500
1. paymentDetailsService.getUsersPaymentDetails(user) — must exist
2. wallet.balance >= amount (InsufficientBalanceException)
3. Deduct from wallet.balance (lock funds immediately)
4. Create Withdrawal(PENDING, amount, userId, date=now)
5. save to DB
```

### 9.2 Approve Withdrawal (Admin)

```
PATCH /api/admin/withdrawal/{id}/approve
1. assertAdmin(user) — role must be ROLE_ADMIN
2. assertPending(withdrawal) — status must be PENDING
3. withdrawal.status = SUCCESS
4. withdrawal.approvedAt = now, approvedBy = admin.email
5. save
6. publishWithdrawalNotification → Kafka "notification-events"
```

### 9.3 Reject Withdrawal (Admin)

```
PATCH /api/admin/withdrawal/{id}/reject
1. assertAdmin, assertPending
2. Refund: ownerWallet.balance += withdrawalAmount (save)
3. withdrawal.status = DECLINE
4. withdrawal.approvedAt, approvedBy set
5. save
6. publishWithdrawalNotification → Kafka "notification-events"
```

### 9.4 Business Rules

- Only PENDING withdrawals can be approved/rejected (prevents double-processing)
- Funds are locked at request time by deducting from balance
- Rejection refunds the full amount to the owner's wallet (not the admin's)
- Notification published via Kafka is fire-and-forget (non-critical; failure logs warning)

---

## 10. Payment Module

### 10.1 Razorpay Flow

```
1. POST /api/payment/razorpay/amount/{amount}
   → createRazorpayPaymentLink(user, amount, orderId)
   → returns short_url
2. User completes payment on Razorpay
3. Razorpay redirects to http://localhost:3001/wallet?order_id={id}
4. Frontend calls POST /api/payment/proceed?paymentId=pay_xxx&orderId=123
   → ProceedPaymentOrder(paymentOrder, paymentId)
   → Razorpay.payments.fetch(paymentId) → status == "captured" → SUCCESS
   → walletService.addBalance(user, amount)
```

### 10.2 Stripe Flow

Same as Razorpay but uses Stripe Sessions. Amount converted to cents (×100).

---

## 11. Notification Module

Notifications are always delivered via Kafka (decoupled from the triggering transaction):

| Trigger | Type Claim | Subject |
|---|---|---|
| BUY/SELL executed | `TRADE` | "Trade Executed: BUY BTC" |
| Price alert triggered | `ALERT` | "Price Alert: BTC above $70,000" |
| Withdrawal approved | `WITHDRAWAL_APPROVED` | "Withdrawal Approved ✅" |
| Withdrawal rejected | `WITHDRAWAL_REJECTED` | "Withdrawal Rejected ❌" |

Delivery: HTML email (MimeMessage) + WebSocket push (`/queue/notifications`)

---

## 12. WebSocket Real-Time Pipeline

```
Frontend subscribes to:
  /topic/prices/{coinId}   → per-coin price stream
  /topic/prices            → global price broadcast
  /user/queue/notifications → personal trade/alert notifications

Flow:
Binance WS → PriceUpdateProducer → Kafka "price-updates"
  → PriceUpdateConsumer
      → Redis cache update
      → SimpMessagingTemplate.convertAndSend("/topic/prices/bitcoin", priceData)
      → SimpMessagingTemplate.convertAndSend("/topic/prices", priceData)
      → checkPriceAlerts()

Notification flow:
NotificationConsumer → SimpMessagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload)
```

**WebSocket Config:**
- STOMP endpoint: `/ws`
- Allowed origins: configured in `WebSocketConfig.java`
- Message broker destinations: `/topic`, `/user`

---

## 13. Analytics Module

Located in `com.jeevan.TradingApp.analytics` package with sub-packages: `consumer`, `controller`, `dto`, `model`, `producer`, `repository`, `service`.

Publishes/consumes from `analytics-events` topic. Handles portfolio snapshots, metrics computation, and historical analytics.

---

## 14. Unit Testing Guide

### Dependencies to add (Maven/Gradle)

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.mockito</groupId>
  <artifactId>mockito-core</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka-test</artifactId>
  <scope>test</scope>
</dependency>
```

---

### 14.1 `OtpService` Unit Tests

**What to mock:** `RedisTemplate`

**Test cases:**
```
✅ generateAndStoreOtp — happy path: stores otp, sets cooldown, returns 6-digit string
✅ generateAndStoreOtp — when cooldown key exists: throws CustomException("OTP_COOLDOWN")
✅ verifyOtp — correct OTP on first attempt: returns true, clears all keys
✅ verifyOtp — wrong OTP: throws CustomException("INVALID_OTP"), increments attempts
✅ verifyOtp — expired OTP (key not in Redis): throws CustomException("OTP_EXPIRED")
✅ verifyOtp — after 3 failed attempts: throws CustomException("OTP_MAX_ATTEMPTS")
✅ canResendOtp — no cooldown key: returns true
✅ canResendOtp — cooldown active: returns false
✅ clearOtp — deletes all 3 Redis keys
```

**Example skeleton:**
```java
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOps;
    @InjectMocks OtpService otpService;

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void generateAndStoreOtp_happyPath_returnsOtp() {
        when(redisTemplate.hasKey("otp_cooldown:test@gmail.com")).thenReturn(false);
        String otp = otpService.generateAndStoreOtp("test@gmail.com");
        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
        verify(valueOps, times(3)).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void verifyOtp_correctOtp_returnsTrue() {
        when(valueOps.get("otp_attempts:test@gmail.com")).thenReturn("0");
        when(valueOps.get("otp:test@gmail.com")).thenReturn("123456");
        assertTrue(otpService.verifyOtp("test@gmail.com", "123456"));
        verify(redisTemplate, times(3)).delete(anyString());
    }
}
```

---

### 14.2 `MarketDataCacheService` Unit Tests

**What to mock:** `RedisTemplate`, `HashOperations`

**Test cases:**
```
✅ setPrice — stores all fields in hash, sets TTL
✅ getPrice — returns map if hash exists
✅ getPrice — returns null if hash is empty (expired)
✅ getAllPrices — scans keys, aggregates all hashes
✅ getAllPrices — returns empty list if no keys
✅ hasPriceInCache — returns true if key exists, false otherwise
```

---

### 14.3 `OrderServiceImpl` Unit Tests

**What to mock:** `OrderRepository`, `WalletService`, `FeeService`, `OrderItemRepository`, `AssetService`, `TradeEventProducer`, `RiskValidationService`

**Test cases:**
```
✅ buyAsset — valid qty, sufficient balance → returns FILLED order, calls tradeEventProducer.publish
✅ buyAsset — qty <= 0 → throws OrderValidationException
✅ buyAsset — insufficient balance → walletService.payOrderPayment throws InsufficientBalanceException
✅ buyAsset — new asset → assetService.createAsset called
✅ buyAsset — existing asset → assetService.updateAsset called
✅ sellAsset — valid: credits wallet, updates asset quantity
✅ sellAsset — no asset → throws ResourceNotFoundException
✅ sellAsset — insufficient asset qty → throws OrderValidationException
✅ sellAsset — dust cleanup: if remaining value < $1 → deleteAsset called
✅ processOrder — BUY type → delegates to buyAsset
✅ processOrder — SELL type → delegates to sellAsset
✅ processOrder — invalid type → throws OrderValidationException
✅ cancelOrder — FILLED order → throws OrderValidationException
✅ cancelOrder — wrong user → throws UnauthorizedAccessException
✅ cancelOrder — BUY OPEN → calls walletService.releaseLock
```

---

### 14.4 `WalletServiceImpl` Unit Tests

**What to mock:** `WalletRepository`, `LedgerService`, `FeeService`, `UserRepository`

**Test cases:**
```
✅ getUserWallet — existing wallet returned
✅ getUserWallet — no wallet: creates new with ZERO balance
✅ addBalance — creates CREDIT ledger entry, increments balance
✅ debit — decrements balance, creates DEBIT ledger entry
✅ credit — increments balance, creates CREDIT ledger entry
✅ releaseLock — decrements lockedBalance, creates TRADE_RELEASE entry
✅ payOrderPayment BUY — checks available balance, creates TRADE_LOCK, increments lockedBalance
✅ payOrderPayment BUY — insufficient balance → throws InsufficientBalanceException
✅ walletToWalletTransfer — debits sender, credits receiver
✅ walletToWalletTransfer — insufficient sender balance → throws InsufficientBalanceException
```

---

### 14.5 `WithdrawalServiceImpl` Unit Tests

**What to mock:** `WalletService`, `WalletRepository`, `WithdrawalRepository`, `PaymentDetailsService`, `NotificationEventProducer`

**Test cases:**
```
✅ requestWithdrawal — no payment details → throws CustomException("PAYMENT_DETAILS_MISSING")
✅ requestWithdrawal — insufficient balance → throws InsufficientBalanceException
✅ requestWithdrawal — success: deducts balance, saves PENDING withdrawal
✅ approveWithdrawal — non-admin → throws UnauthorizedAccessException
✅ approveWithdrawal — not PENDING → throws CustomException("WITHDRAWAL_ALREADY_PROCESSED")
✅ approveWithdrawal — success: status=SUCCESS, approvedAt set, notification published
✅ rejectWithdrawal — not PENDING → throws CustomException("WITHDRAWAL_ALREADY_PROCESSED")
✅ rejectWithdrawal — success: refunds owner wallet, status=DECLINE, notification published
✅ rejectWithdrawal — notification publish fails: does NOT throw (non-critical)
```

---

### 14.6 `TradeEventConsumer` Unit Tests

**What to mock:** `ProcessedEventRepository`, `NotificationEventProducer`, `TransactionEventProducer`

**Test cases:**
```
✅ consume — duplicate eventId (already in ProcessedEvent): skips, no producers called
✅ consume — new event: publishes NotificationEvent + TransactionEvent, saves ProcessedEvent
✅ consume — BUY order: TransactionEvent.transactionType == "DEBIT"
✅ consume — SELL order: TransactionEvent.transactionType == "CREDIT"
✅ consume — exception in body: re-throws (for retry)
✅ buildTradeEmailBody — BUY: HTML contains "purchased", green color
✅ buildTradeEmailBody — SELL: HTML contains "sold", red color
```

---

### 14.7 `PriceUpdateConsumer` Unit Tests

**What to mock:** `SimpMessagingTemplate`, `MarketDataCacheService`, `PriceAlertRepository`, `PriceAlertService`

**Test cases:**
```
✅ consume — updates Redis cache with coinId as key
✅ consume — sends to /topic/prices/{coinId}
✅ consume — sends to /topic/prices (global)
✅ consume — no active alerts: PriceAlertService.triggerAlert NOT called
✅ consume — ABOVE alert: price >= target → triggerAlert called
✅ consume — BELOW alert: price <= target → triggerAlert called
✅ consume — alert target not met: triggerAlert NOT called
✅ consume — exception does NOT propagate (swallowed + logged)
```

---

### 14.8 `NotificationConsumer` Unit Tests

**What to mock:** `ProcessedEventRepository`, `JavaMailSender`, `SimpMessagingTemplate`

**Test cases:**
```
✅ consume — duplicate: skips, no email sent
✅ consume — sends HTML email to correct address
✅ consume — pushes WebSocket notification to correct user path
✅ consume — saves ProcessedEvent after successful delivery
✅ consume — JavaMailSender throws: re-throws RuntimeException (triggers DLT)
```

---

### 14.9 `CoinServiceImpl` Unit Tests

**What to mock:** `CoinRepository`, `ObjectMapper`, `MarketDataCacheService`, `RedisTemplate`

**Test cases:**
```
✅ getCoinList — Redis hot cache hit: returns enriched coin list (no CoinGecko call)
✅ getCoinList — Redis empty: fallback to fetchCoinListFromCoinGecko
✅ getMarketChart — Redis cache hit: returns cached JSON, skip HTTP call
✅ getMarketChart — Rate limiter triggered: getCoinDetailsFallback serves stale cache
✅ getMarketChart — Rate limited + no cache: throws CustomException("API_ERROR")
✅ findById — coin not found in DB: throws CustomException("COIN_NOT_FOUND")
```

---

## 15. Integration Testing Guide

### 15.1 Test Slices & Annotations

Use these Spring Boot test annotations:

| Annotation | When to Use |
|---|---|
| `@SpringBootTest` | Full context integration tests |
| `@WebMvcTest(Controller.class)` | Controller-only tests with MockMvc |
| `@DataJpaTest` | Repository-layer tests with H2 |
| `@EmbeddedKafka` | Kafka producer/consumer integration |
| `@TestPropertySource` | Override Redis/Kafka hosts for test |

### 15.2 Redis Integration Tests

Use Testcontainers or `@EmbeddedRedis`:

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.redis.host=localhost",
    "spring.redis.port=6379"
})
class OtpServiceIntegrationTest {
    // Uses testcontainers-redis or a local Redis instance

    @Autowired OtpService otpService;

    @Test
    void fullOtpFlow_generateVerify_success() {
        String otp = otpService.generateAndStoreOtp("user@test.com");
        assertTrue(otpService.verifyOtp("user@test.com", otp));
    }

    @Test
    void cooldownPreventsImmediateResend() {
        otpService.generateAndStoreOtp("user@test.com");
        assertThrows(CustomException.class,
            () -> otpService.generateAndStoreOtp("user@test.com"));
    }
}
```

### 15.3 Kafka Integration Tests

```java
@SpringBootTest
@EmbeddedKafka(partitions = 3, topics = {
    "trade-events", "notification-events", "transaction-events"
})
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
class TradeEventFlowIntegrationTest {

    @Autowired TradeEventProducer tradeEventProducer;
    @Autowired ProcessedEventRepository processedEventRepository;

    @Test
    void publishAndConsume_tradeEvent_savesProcessedEvent() throws InterruptedException {
        TradeEvent event = TradeEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .orderType("BUY")
            .userId(1L)
            .userEmail("test@test.com")
            .coinSymbol("btc")
            .quantity(0.1)
            .price(BigDecimal.valueOf(5000))
            .orderId(100L)
            .status("FILLED")
            .timestamp(LocalDateTime.now())
            .build();

        tradeEventProducer.publish(event);

        // Wait for consumer to process
        Thread.sleep(3000);

        assertTrue(processedEventRepository.existsByEventId(event.getEventId()));
    }

    @Test
    void duplicateTradeEvent_skipsProcessing() throws InterruptedException {
        // Pre-save a processed event
        processedEventRepository.save(new ProcessedEvent("existing-event-id"));

        TradeEvent event = TradeEvent.builder()
            .eventId("existing-event-id")
            // ... fields
            .build();

        tradeEventProducer.publish(event);
        Thread.sleep(2000);

        // Verify no duplicate NotificationEvent was created
        // (assert via mock or check secondary effects)
    }
}
```

### 15.4 Controller Integration Tests (MockMvc)

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean OrderService orderService;
    @MockBean UserService userService;

    @Test
    void buyOrder_validRequest_returns200() throws Exception {
        // Mock authentication header with a valid JWT
        mockMvc.perform(
            post("/api/order/pay")
                .param("orderType", "BUY")
                .param("coinId", "bitcoin")
                .param("quantity", "0.5")
                .header("Authorization", "Bearer {valid_test_token}")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void buyOrder_noAuth_returns401() throws Exception {
        mockMvc.perform(
            post("/api/order/pay")
                .param("orderType", "BUY"))
            .andExpect(status().isUnauthorized());
    }
}
```

### 15.5 Repository Integration Tests (`@DataJpaTest`)

```java
@DataJpaTest
class ProcessedEventRepositoryTest {

    @Autowired ProcessedEventRepository repo;

    @Test
    void existsByEventId_existingId_returnsTrue() {
        ProcessedEvent pe = new ProcessedEvent();
        pe.setEventId("test-event-123");
        repo.save(pe);
        assertTrue(repo.existsByEventId("test-event-123"));
    }

    @Test
    void existsByEventId_nonExistingId_returnsFalse() {
        assertFalse(repo.existsByEventId("non-existent-id"));
    }
}
```

### 15.6 End-to-End BUY Order Scenario Test

```
Test Scenario: User buys Bitcoin

1. SETUP: Create user, wallet with $10,000 balance, Bitcoin coin entity in DB
2. POST /api/order/pay?orderType=BUY&coinId=bitcoin&quantity=0.01 (JWT auth)
3. ASSERT:
   a. Response 200 OK, Order.status = FILLED
   b. Wallet.balance reduced by (qty * price + fee)
   c. Asset created for user with quantity 0.01
   d. Kafka "trade-events" topic received 1 message with orderId
   e. After consumer completes: ProcessedEvent saved for tradeEventId
   f. "audit_log" table has 1 row for the order
```

### 15.7 Withdrawal Admin Approval Scenario Test

```
Test Scenario: Admin approves a withdrawal

1. SETUP: User with $1000 balance requests withdrawal of $500
   → wallet.balance = $500 (locked $500)
2. Admin calls PATCH /api/admin/withdrawal/{id}/approve
3. ASSERT:
   a. Withdrawal.status = SUCCESS
   b. Withdrawal.approvedAt populated
   c. Kafka "notification-events" received WITHDRAWAL_APPROVED
   d. User email sent (mock JavaMailSender)
4. Also test reject:
   → PATCH /api/admin/withdrawal/{id}/reject
   → wallet.balance = $1000 (refunded)
   → Withdrawal.status = DECLINE
```

### 15.8 Price Alert Trigger Scenario

```
Test Scenario: Price alert fires when price crosses threshold

1. Create PriceAlert for user: coin=bitcoin, ABOVE, targetPrice=$70,000
2. Publish PriceUpdateEvent via PriceUpdateProducer with currentPrice=$71,000
3. PriceUpdateConsumer processes event
4. ASSERT:
   a. priceAlertService.triggerAlert(alert, $71,000) was called
   b. "notification-events" topic received a ALERT-type message
   c. PriceAlert.triggered = true in DB
```

---

## Appendix: Key Redis Key Patterns Reference

```
# OTP
otp:{email}                     → 6-digit OTP, 5-min TTL
otp_attempts:{email}            → int counter, 5-min TTL
otp_cooldown:{email}            → boolean, 30-sec TTL

# Market Price (Hash)
market:price:{coinId}           → Hash {coinId,symbol,name,currentPrice,...}, 5-min TTL

# CoinGecko API Responses (String/JSON)
market:chart:{coinId}:{days}    → JSON String, 5-min TTL
market:details:{coinId}         → JSON String, 5-min TTL
market:top50                    → JSON String, 15-min TTL
market:trending                 → JSON String, 15-min TTL
```

## Appendix: Kafka Topic Summary

| Topic | Key | Producers | Consumers | Group |
|---|---|---|---|---|
| `trade-events` | orderId | `TradeEventProducer` | `TradeEventConsumer` | `trade-group` |
| `price-updates` | coinId | `PriceUpdateProducer` | `PriceUpdateConsumer` | `price-group` |
| `notification-events` | userId | `NotificationEventProducer` | `NotificationConsumer` | `notification-group` |
| `transaction-events` | userId | `TransactionEventProducer` | `TransactionConsumer` | `txn-audit-group` |
| `analytics-events` | — | Analytics module | Analytics module | — |
