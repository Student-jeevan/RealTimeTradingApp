# WebSocket Integration — Walkthrough

## What Was Built

Real-time push from server → browser using Spring WebSocket (STOMP) + SockJS + @stomp/stompjs.

---

## Architecture

```
Browser (React)
   │  SockJS connect → ws://localhost:8081/ws
   │
   ▼
Spring Boot (STOMP broker)
   ├── /topic/orderbook/{coinId}  ← broadcast to all subscribers
   └── /user/{userId}/queue/orders ← targeted to one user
   
OrderBookServiceImpl
   ├── after placeOrder()   → WebSocketService.pushOrderBookUpdate()
   └── after each match     → WebSocketService.pushUserOrderUpdate() x2
```

---

## Backend Changes

| File | Change |
|------|--------|
| [pom.xml](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/backend/pom.xml) | Added `spring-boot-starter-websocket` |
| [WebSocketConfig.java](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/backend/src/main/java/com/jeevan/TradingApp/config/WebSocketConfig.java) *(new)* | STOMP endpoint `/ws` with SockJS, in-memory broker for `/topic` + `/user` |
| [WebSocketService.java](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/backend/src/main/java/com/jeevan/TradingApp/service/WebSocketService.java) *(new)* | Wraps `SimpMessagingTemplate`; two push methods |
| [AppConfig.java](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/backend/src/main/java/com/jeevan/TradingApp/config/AppConfig.java) | Added `.requestMatchers("/ws/**").permitAll()` |
| [OrderBookServiceImpl.java](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/backend/src/main/java/com/jeevan/TradingApp/service/OrderBookServiceImpl.java) | Injected [WebSocketService](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/backend/src/main/java/com/jeevan/TradingApp/service/WebSocketService.java#20-50); pushes after [placeOrder](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/backend/src/main/java/com/jeevan/TradingApp/service/OrderBookServiceImpl.java#54-98) and per matched pair |

---

## Frontend Changes

| File | Change |
|------|--------|
| [lib/websocket.js](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/frontend/src/lib/websocket.js) *(new)* | Singleton STOMP client — `connect / subscribe / unsubscribe / disconnect` |
| [hooks/useUserOrderUpdates.js](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/frontend/src/hooks/useUserOrderUpdates.js) *(new)* | Hook: subscribes to `/user/queue/orders`, dispatches Redux action + toast |
| [pages/Order/OrderBookPanel.jsx](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/frontend/src/pages/Order/OrderBookPanel.jsx) | Replaced `setInterval` polling with STOMP `/topic/orderbook/{coinId}` subscription |
| `pages/Stock Detials/TradingForm.jsx` | Calls [useUserOrderUpdates()](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/frontend/src/hooks/useUserOrderUpdates.js#7-55) for instant fill notifications |
| [State/Order/ActionTypes.js](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/frontend/src/State/Order/ActionTypes.js) | Added `WEBSOCKET_ORDER_UPDATE` |
| [State/Order/Reducer.js](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/frontend/src/State/Order/Reducer.js) | Upserts real-time order into `orders[]` without a REST call |

---

## STOMP Topics

| Destination | Direction | Who subscribes |
|---|---|---|
| `/topic/orderbook/{coinId}` | Server → all | [OrderBookPanel](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/frontend/src/pages/Order/OrderBookPanel.jsx#5-110) per coin page |
| `/user/queue/orders` | Server → one user | [useUserOrderUpdates](file:///c:/Users/hp/OneDrive/Desktop/TradingAppApplication/frontend/src/hooks/useUserOrderUpdates.js#7-55) hook |

---

## How to Test

1. `docker-compose up -d` (MySQL + Redis)
2. `cd backend && mvn spring-boot:run`
3. `cd frontend && npm run dev`
4. Sign in, go to any coin detail page
5. Open **Trade** dialog → order book shows live pulsing dots
6. In browser DevTools → Network → **WS** tab: confirm WS connection to `/ws/...`
7. Place a limit BUY order that matches an open SELL:
   - **Order Book** panel updates instantly (no page refresh)
   - Toast notification appears: `✅ Order #X FILLED — BUY 0.1 BTC`
8. The Redux `order.orders` array in DevTools updates automatically via `WEBSOCKET_ORDER_UPDATE`
