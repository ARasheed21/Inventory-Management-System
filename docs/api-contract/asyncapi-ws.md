# WebSocket / STOMP Contract

Real-time updates are delivered over **STOMP over SockJS** and are intentionally outside the
OpenAPI document (`openapi.yaml`). This file is the authoritative contract for that channel.

## Endpoint

```
ws(s)://<host>/api/ws            (SockJS; clients may also connect directly under /api/ws/<server>...)
```

- Browser/web: `stompjs` + `sockjs-client`
- Flutter/Dart: `stomp_dart_client`

## CONNECT (authentication)

The STOMP CONNECT frame MUST carry the access token as a raw header:

```
CONNECT
accept-version:1.2
host:<host>
Authorization:Bearer <accessToken>

\0
```

Without the header the TCP/SockJS connection opens, but the session is anonymous: no messages
are ever delivered to `/user/queue/...` for it.

## Destinations a client subscribes to

| Destination | Audience | Purpose |
|---|---|---|
| `/user/queue/orders` | authenticated user (private) | status changes for this user's orders |
| `/topic/orders/{orderId}` | anyone subscribed (public broadcast) | live order timeline views |

## Server-pushed message payloads (JSON)

All messages are JSON objects with at least `orderId` and `status`.

| `status` value | When sent | Extra fields |
|---|---|---|
| `PAID` | order payment succeeds | – |
| `SHIPPED` | warehouse ships the order | – |
| `DELIVERED` | warehouse marks delivery | – |
| `RESERVATION_EXPIRED` | timeout job cancels an expired pending order owned by the connected user | – |
| `PAYMENT_FAILED` | payment attempt fails (expired reservation or invalid state) | `reason` (string) |
| `USER_UPDATE` / test payloads | admin notify endpoints (testing only) | – |

Example:

```json
{ "orderId": "3f2a...", "status": "PAYMENT_FAILED", "reason": "Reservation expired for order: 3f2a..." }
```

## Client-side guidance

1. Subscribe immediately after CONNECT (before triggering actions) to avoid missing pushes.
2. Treat pushes as hints: after receiving one, re-fetch the affected resource via REST
   (`GET /api/orders/{id}`) to render authoritative data including
   `reservationSecondsRemaining`.
3. Reconnect with backoff on socket close; re-send the CONNECT auth header each time.
4. Tokens expire (default 900s). On reconnect failure with 401/unauthorized frame, refresh via
   `POST /auth/refresh` before reconnecting.

## Reference implementation

`src/test/java/com/example/inventory/web/WebSocketIntegrationTest.java` demonstrates the full
flow (login → CONNECT → SUBSCRIBE → receive push) against the running application.
