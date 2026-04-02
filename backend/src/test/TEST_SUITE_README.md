# TradingApp — Test Suite Structure & Setup Guide
## Project Test Directory Layout
```
backend/src/test/
├── java/com/jeevan/TradingApp/
│   │
│   ├── config/
│   │   └── RedisTestConfig.java          ← Embedded Redis for unit tests
│   │
│   ├── testutil/
│   │   └── TestDataBuilder.java          ← Central test data factory (all tests use this)
│   │
│   ├── unit/
│   │   ├── service/
│   │   │   ├── OtpServiceTest.java                ← 15 tests (Redis mocked)
│   │   │   ├── MarketDataCacheServiceTest.java     ← 14 tests (Redis mocked)
│   │   │   ├── OrderServiceImplTest.java           ← 18 tests (DB/Kafka mocked)
│   │   │   ├── WalletServiceImplTest.java          ← 14 tests (DB mocked)
│   │   │   └── WithdrawalServiceImplTest.java      ← 16 tests (DB/Kafka mocked)
│   │   │
│   │   └── kafka/
│   │       ├── TradeEventConsumerTest.java         ← 14 tests (repos mocked)
│   │       ├── PriceUpdateConsumerTest.java        ← 16 tests (WS/Redis mocked)
│   │       └── NotificationConsumerTest.java       ← 15 tests (mail/WS mocked)
│   │
│   └── integration/
│       ├── BaseIntegrationTest.java               ← Testcontainers base (MySQL+Redis+Kafka)
│       ├── OrderExecutionIntegrationTest.java     ← BUY/SELL full flow
│       ├── WithdrawalIntegrationTest.java         ← Request/Approve/Reject flow
│       ├── RedisIntegrationTest.java              ← OTP + cache round-trips
│       └── KafkaEventPipelineIntegrationTest.java ← Event producer → consumer → DB
│
└── resources/
    └── application-test.yml                      ← Test-specific Spring config
```

---

## What Each Test Category Does

| Category | Containers Used | Speed | Purpose |
|---|---|---|---|
| **Unit tests** | None (all mocked) | < 5 sec | Verify business logic in isolation |
| **Integration tests** | MySQL + Redis + Kafka | 30–90 sec | Verify real interactions between components |

---

## Running the Tests

### Run only unit tests (fast — CI pre-push)
```bash
cd backend
mvn test -Dgroups="unit" -pl backend
# OR simply exclude integration tests
mvn test -Dtest="**/unit/**/*Test" -pl backend
```

### Run only integration tests (slower — requires Docker)
```bash
mvn test -Dtest="**/integration/**/*Test" -pl backend
```

### Run all tests
```bash
mvn verify -pl backend
```

### Run a single test class
```bash
mvn test -Dtest=OtpServiceTest -pl backend
```

---

## Infrastructure Requirements

### Unit Tests
- **No external infrastructure needed** — all services mocked via Mockito
- Just JDK 17 + Maven

### Integration Tests
- **Docker must be running** (Testcontainers pulls images automatically)
- Images used:
  - `mysql:8.0.33`
  - `redis:7-alpine`
  - `confluentinc/cp-kafka:7.5.0`
- First run downloads images (~1 GB total). Subsequent runs use Docker cache.
- Container reuse enabled (`withReuse(true)`) — subsequent test runs start in < 5 sec.

---

## Key Design Decisions

### 1. TestDataBuilder (shared test data factory)
All test data created through `TestDataBuilder` static methods to:
- Avoid duplicated magic literals across 100+ test methods
- Keep test setup readable (`TestDataBuilder.buildCustomer()`, etc.)
- Make data consistent between unit and integration tests

### 2. Idempotency Testing Pattern
Every Kafka consumer idempotency test follows:
```java
// 1. Pre-save the eventId into processed_events
processedEventRepository.save(new ProcessedEvent(null, eventId, null));

// 2. Publish duplicate event
producer.publish(event);

// 3. Assert side-effects did NOT occur (no email, no DB insert, no counter change)
await().during(3, SECONDS).atMost(4, SECONDS)
       .untilAsserted(() -> assertThat(repository.count()).isEqualTo(before));
```

### 3. Awaitility for Async Kafka Assertions
All integration tests that verify Kafka consumer side-effects use:
```java
await().atMost(10, TimeUnit.SECONDS)
       .pollInterval(300, TimeUnit.MILLISECONDS)
       .untilAsserted(() -> assertThat(repo.findAll()).isNotEmpty());
```
This eliminates brittle `Thread.sleep()` calls and makes tests self-timing.

### 4. @MockBean for External APIs
Integration tests mock `JavaMailSender` via `@MockitoBean` (Spring Boot 3.4+) to:
- Prevent real SMTP calls in CI
- Still test that `NotificationConsumer` invokes the sender correctly
- Allow WebSocket tests without actual STOMP connections

### 5. Container Reuse
`withReuse(true)` on each container means the container is started once per JVM
(or per Docker daemon session). This cuts integration test startup from ~45s to ~3s
on repeated runs.

---

## Test Count Summary

| File | Tests |
|---|---|
| OtpServiceTest | 11 |
| MarketDataCacheServiceTest | 14 |
| OrderServiceImplTest | 18 |
| WalletServiceImplTest | 14 |
| WithdrawalServiceImplTest | 16 |
| TradeEventConsumerTest | 14 |
| PriceUpdateConsumerTest | 16 |
| NotificationConsumerTest | 15 |
| OrderExecutionIntegrationTest | 7 |
| WithdrawalIntegrationTest | 8 |
| RedisIntegrationTest | 10 |
| KafkaEventPipelineIntegrationTest | 8 |
| **TOTAL** | **~151** |

---

## Adding New Tests

1. **New service** → add unit tests in `unit/service/YourServiceTest.java`
2. **New Kafka consumer** → add unit tests in `unit/kafka/YourConsumerTest.java`
3. **New feature flow** → add integration test extending `BaseIntegrationTest`
4. **New test entity** → add builder method to `TestDataBuilder`
