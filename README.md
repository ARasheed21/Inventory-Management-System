# Inventory Management System

A Spring Boot 3 backend for an e-commerce inventory platform: product catalog, customer shopping cart, order lifecycle, and real-time order updates over WebSocket. Built as a modular monolith (domain / application / infrastructure / web layers) with JWT-based security and an OpenAPI-documented REST API.

## Features

- **Authentication** — `POST /auth/register` (self-service signup returning tokens), `POST /auth/login`, `POST /auth/refresh`, `GET /auth/me` returning JWT access + refresh tokens (HS256 by default, RS256 optional). Tokens carry `userId`, `email`, and `ROLE_`-prefixed `roles`. Accounts are persisted in the database with BCrypt-hashed passwords.
- **Security hardening** — password policy on registration (min 8 chars, letter + digit), per-username login rate limiting (returns `429 Too Many Requests` after repeated failures), and a startup guard that refuses to boot with the default `jwt.secret` under production profiles.
- **Product catalog** — paginated, searchable, category-filterable browsing (`GET /api/products`) and product details (`GET /api/products/{id}`). Admin-only create/update via `/api/inventory/products`, with every stock change captured in an Envers audit trail.
- **Shopping cart** — CRUD under `/api/cart`, scoped to the authenticated customer.
- **Orders** — place orders, pay, cancel, query status/history. Customers can only see and act on their own orders; server derives ownership from the JWT.
- **Warehouse fulfillment** — `POST /api/orders/{id}/ship` and `POST /api/orders/{id}/deliver` (WAREHOUSE/ADMIN only) drive orders through PAID → SHIPPED → DELIVERED; invalid transitions return `409`.
- **Reservation UX** — every order response carries a server-computed `reservationSecondsRemaining`; expired reservations cancel automatically and push `RESERVATION_EXPIRED` to the owner's WebSocket queue. Paying past the reservation window fails with `409` and pushes `PAYMENT_FAILED`.
- **Audit history** — admin-only revision history for products and orders via `GET /api/admin/audit/products/{id}` and `GET /api/admin/audit/orders/{id}`.
- **Reserved inventory report** — `GET /api/inventory/reserved` (WAREHOUSE/ADMIN) shows per-product stock, reserved-by-pending-orders, and available quantities.
- **Real-time updates** — STOMP over WebSocket at `/api/ws` with JWT authentication at CONNECT; user queue `/user/queue/orders` and topic `/topic/orders`.
- **Consistent error format** — every error (including security 401/403) returns `{timestamp, status, error, message, path}`.
- **OpenAPI/Swagger** — interactive docs generated from controller annotations.

## Technology stack

- Java 21
- Spring Boot 3.5.x (Web, Data JPA, Security, Validation, WebSocket, Actuator)
- Spring Security with OAuth2 Resource Server (JWT)
- PostgreSQL (default runtime) or H2 in-memory (local/test profiles)
- Springdoc OpenAPI / Swagger UI
- Maven, Docker Compose

## Prerequisites

- Java 21 or later (`java -version` must report 21)
- Maven 3.9+
- Docker Desktop (for the recommended PostgreSQL setup) — or none if you use the H2 local profile

> If you see `UnsupportedClassVersionError` or "class file version" errors, your JDK is too old — install JDK 21 and reopen your terminal.

## Run the application

### Option 1: PostgreSQL via Docker (recommended)

```bash
docker compose up -d          # postgres:16 on localhost:5432 (inventory_db / inventory / inventory)
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### Option 2: In-memory H2 (no database installation needed)

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=dev,local"
```

Data does not persist across restarts with this profile.

### If the default port (8080) is already taken

Append a different port, e.g. 8081:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=dev,local" "-Dspring-boot.run.arguments=--server.port=8081"
```

All examples below assume the server runs on **8081**; substitute your actual port where needed.

## Open the application

The API is served under the context path `/api`:

| URL | Description |
|---|---|
| http://localhost:8081/api/swagger-ui.html | Swagger UI |
| http://localhost:8081/api/v3/api-docs | OpenAPI JSON |
| http://localhost:8081/api/health | Health endpoint |
| ws://localhost:8081/api/ws | STOMP endpoint (SockJS) |

## Default credentials

Database-persisted accounts seeded at startup (only if the `accounts` table is empty); they survive restarts. New customers can self-register via `POST /auth/register`.

| User | Password | Roles |
|---|---|---|
| `admin` | `admin` | `ROLE_ADMIN` |
| `warehouse` | `warehouse` | `ROLE_WAREHOUSE` |
| `customer` | `customer` | `ROLE_CUSTOMER` |

## Manual API walkthrough

### 1. Register or login and get a token

```powershell
# Self-service registration (returns tokens immediately; password needs 8+ chars with a letter and a digit)
$newUser = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/auth/register `
  -ContentType "application/json" -Body '{"username":"alice","email":"alice@example.com","password":"s3cret-pass"}'

# Or login with an existing account
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/auth/login `
  -ContentType "application/json" -Body '{"username":"customer","password":"customer"}'
$token = $login.accessToken

Invoke-RestMethod http://localhost:8081/api/auth/me -Headers @{Authorization="Bearer $token"}
# -> userId, username, email, roles = ["ROLE_CUSTOMER"]
```

Bad credentials return HTTP 401; duplicate username/email returns 409; weak passwords return 400.

### 2. Create products (admin) and browse (anyone)

```powershell
$admin = (Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/auth/login `
  -ContentType "application/json" -Body '{"username":"admin","password":"admin"}').accessToken

$created = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/inventory/products `
  -Headers @{Authorization="Bearer $admin"} -ContentType "application/json" -Body (@"
{"name":"Desk Lamp","description":"Compact lamp","price":"45.00","currency":"USD",
 "quantityInStock":25,"category":"lighting"}
"@)
$productId = $created.id        # <- use THIS id in the examples below

# Paginated catalog with search and category filter -> {content,total,page,size}
Invoke-RestMethod "http://localhost:8081/api/products?search=lamp&category=lighting&page=0&size=20" `
  -Headers @{Authorization="Bearer $token"}

# Product details
Invoke-RestMethod "http://localhost:8081/api/products/$productId" -Headers @{Authorization="Bearer $token"}

# Non-existent product -> 404 with the standard error body
Invoke-RestMethod "http://localhost:8081/api/products/does-not-exist" -Headers @{Authorization="Bearer $token"}
```

### 3. Shopping cart (as customer)

```powershell
$item = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/cart `
  -Headers @{Authorization="Bearer $token"} -ContentType "application/json" `
  -Body ('{"productId":"' + $productId + '","quantity":2}')

Invoke-RestMethod http://localhost:8081/api/cart -Headers @{Authorization="Bearer $token"}

Invoke-RestMethod -Method Put -Uri "http://localhost:8081/api/cart/$($item.id)" `
  -Headers @{Authorization="Bearer $token"} -ContentType "application/json" -Body '{"quantity":5}'

Invoke-RestMethod -Method Delete -Uri "http://localhost:8081/api/cart/$($item.id)" `
  -Headers @{Authorization="Bearer $token"}
```

### 4. Order lifecycle (as customer)

The server overrides `customerId` with the authenticated username — customers cannot create orders for others.

```powershell
$order = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/orders `
  -Headers @{Authorization="Bearer $token"} -ContentType "application/json" -Body (@"
{"customerId":"customer","items":[{"productId":"$productId","quantity":5}]}
"@)

Invoke-RestMethod "http://localhost:8081/api/orders/status/$($order.id)" -Headers @{Authorization="Bearer $token"}

Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/orders/$($order.id)/payment" `
  -Headers @{Authorization="Bearer $token"}

# Ownership check: another user gets 404 when reading someone else's order
$wh = (Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/auth/login `
  -ContentType "application/json" -Body '{"username":"warehouse","password":"warehouse"}').accessToken
Invoke-RestMethod "http://localhost:8081/api/orders/$($order.id)" -Headers @{Authorization="Bearer $wh"}   # -> 404
```

Admins may pass any `customerId` to `GET /api/orders` and use the notify endpoints:
`POST /api/orders/{id}/notify-user/{username}` (ADMIN-only) pushes a message to that user's `/user/queue/orders` STOMP queue.

### 5. Warehouse fulfillment (as warehouse or admin)

After payment, warehouse staff move the order through the fulfillment pipeline:

```powershell
$wh = (Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/auth/login `
  -ContentType "application/json" -Body '{"username":"warehouse","password":"warehouse"}').accessToken

Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/orders/$($order.id)/ship" `
  -Headers @{Authorization="Bearer $wh"}     # -> status SHIPPED (409 if not PAID)

Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/orders/$($order.id)/deliver" `
  -Headers @{Authorization="Bearer $wh"}     # -> status DELIVERED
```

Customers attempting these endpoints receive `403`. Each transition is pushed over WebSocket.

### 6. Audit history and reserved stock (admin/warehouse)

```powershell
# Full revision history of an order (ADD/MOD entries with author, timestamp, status snapshot) - admin only
$admin = (Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/auth/login `
  -ContentType "application/json" -Body '{"username":"admin","password":"admin"}').accessToken
Invoke-RestMethod "http://localhost:8081/api/admin/audit/orders/$($order.id)" -Headers @{Authorization="Bearer $admin"}

# Per-product reserved vs available stock - warehouse/admin
Invoke-RestMethod "http://localhost:8081/api/inventory/reserved" -Headers @{Authorization="Bearer $wh"}
```

### 7. Real-time updates (STOMP)

**When to use WebSocket instead of plain REST calls:**

| Scenario | Use |
|---|---|
| Customer's order page should flip from PENDING to PAID/SHIPPED instantly, without refresh or polling | WebSocket (`/user/queue/orders`) |
| Admin dashboard shows incoming orders / stock changes live across all connected admins | WebSocket (`/topic/orders`) |
| Same user logged in on phone and web — cart/order change on one appears on the other | WebSocket (per-user queue) |
| One-off lookups ("what is this order's status?"), product browsing, checkout submit | REST — simpler, use `GET /api/orders/status/{orderId}` |

Rule of thumb: REST for request/response, WebSocket when the *server* needs to push something the client didn't ask for at that moment.

**Message format:** JSON `{ "orderId": "...", "status": "..." }` — pushed on payment, ship, deliver, reservation expiry (`RESERVATION_EXPIRED`), and payment failure (`PAYMENT_FAILED` with a `reason` field).
**Destinations:** `/user/queue/orders` (private, per user), `/topic/orders` (broadcast).
**Auth:** the STOMP `CONNECT` frame must carry `Authorization: Bearer <accessToken>`.

#### Testing it manually (Node.js, single window)

> **Why not PowerShell?** .NET's `ClientWebSocket` fails the SockJS/Tomcat handshake on many setups
> (`The 'Connection' header value 'upgrade, keep-alive' is invalid`), and hand-rolled STOMP frames are easy to get wrong:
> every frame MUST end with a NUL character (`\0`) or the server silently ignores it. The script below handles both.
> Requires [Node.js](https://nodejs.org) 18+.

**One-time setup:**

```bash
mkdir ws-test && cd ws-test && npm init -y && npm install ws
```

Save as `ws-test.js`. The script is self-driving: it logs in as customer, creates an order,
subscribes to `/user/queue/orders`, then triggers the admin push itself and prints what arrives.

```javascript
// ws-test.js - set BASE to the port your server runs on (default 8080)
const WebSocket = require("ws");
const BASE = "http://localhost:8081";

function extractFrames(raw) {
  if (raw === "o" || raw === "h") return []; // SockJS open / heartbeat
  try { return [].concat(JSON.parse(raw)); }
  catch { // SockJS embeds raw control chars - extract frames tolerantly
    const out = [];
    for (const m of raw.matchAll(/"((?:[^"\\]|\\.)*)"/g)) { try { out.push(JSON.parse(`"${m[1]}"`)); } catch {} }
    return out;
  }
}

async function main() {
  // --- REST setup: login both users, create an order ---
  const post = async (path, body, token) => {
    const res = await fetch(BASE + path, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: body ? JSON.stringify(body) : undefined
    });
    const text = await res.text();
    if (!res.ok || !text.startsWith("{")) {
      console.error(`HTTP ${res.status} on ${path}. Got: ${text.slice(0, 120)}`);
      if (text.startsWith("<")) console.error("You are probably pointing BASE at the wrong port/server.");
      process.exit(1);
    }
    return JSON.parse(text);
  };

  const customer = await post("/api/auth/login", { username: "customer", password: "customer" });
  const admin = (await post("/api/auth/login", { username: "admin", password: "admin" })).accessToken;
  const prods = await (await fetch(`${BASE}/api/products`, { headers: { Authorization: `Bearer ${customer.accessToken}` } })).json();
  if (!prods.content.length) { console.error("No products in DB - create one via Swagger first"); process.exit(1); }
  const order = await post("/api/orders", { customerId: "x", items: [{ productId: prods.content[0].id, quantity: 1 }] }, customer.accessToken);
  console.log("order created:", order.id);

  // --- WebSocket: connect, CONNECT, SUBSCRIBE ---
  const sess = Math.random().toString(36).slice(2, 10); // any unique session id
  const ws = new WebSocket(`ws://localhost:8081/api/ws/000/${sess}/websocket`);
  const send = f => ws.send(JSON.stringify([f + "\0"])); // JSON array + NUL terminator!
  let subscribed = false;

  ws.on("open", () => {
    console.log("sockjs open - sending STOMP CONNECT...");
    send(`CONNECT\naccept-version:1.2\nhost:localhost\nAuthorization:Bearer ${customer.accessToken}\n\n`);
  });

  ws.on("message", m => {
    for (const frame of extractFrames(m.toString())) {
      if (!subscribed && frame.startsWith("CONNECTED")) {
        subscribed = true;
        console.log("STOMP CONNECTED ok - subscribed to /user/queue/orders");
        console.log("listening... waiting for a push (script fires one itself in 500ms)");
        send("SUBSCRIBE\nid:sub-0\ndestination:/user/queue/orders\n\n");
        // fire the push as admin so there is something to receive:
        setTimeout(() =>
          fetch(`${BASE}/api/orders/${order.id}/notify-user/customer`,
            { method: "POST", headers: { Authorization: `Bearer ${admin}` } })
            .then(r => console.log("notify-user status:", r.status)), 500);
      } else if (frame.startsWith("MESSAGE")) {
        console.log("--- PUSH RECEIVED ---");
        console.log(frame);
        process.exit(0);
      }
    }
  });
  ws.on("error", e => { console.error("ws error:", e.message); process.exit(1); });
  setTimeout(() => { console.error("TIMEOUT - no push received after 15s"); process.exit(1); }, 15000);
}
main().catch(e => { console.error(e.message); process.exit(1); });
```

Run it:

```bash
node ws-test.js
```

Expected output:

```
order created: <orderId>
sockjs open - sending STOMP CONNECT...
STOMP CONNECTED ok - subscribed to /user/queue/orders
listening... waiting for a push (script fires one itself in 500ms)
notify-user status: 200
--- PUSH RECEIVED ---
MESSAGE
destination:/user/queue/orders
content-type:application/json
...

{"status":"USER_UPDATE","orderId":"<orderId>"}
```

Notes:
- If you skip the `Authorization` header in CONNECT, the connection opens but no user is authenticated, so nothing arrives on `/user/queue/orders`.
- In the Flutter/JS frontend you won't deal with any of this framing manually — libraries like `stomp_dart_client` (Dart) or `stompjs` + `sockjs-client` (web) handle SockJS wrapping and STOMP framing for you.
- The full flow is also covered by an automated test (`WebSocketIntegrationTest`).

## Run the tests

Full suite (72 tests, uses H2 automatically):

```bash
mvn test
```

Focused integration test:

```bash
mvn -Dtest=InventoryControllerIntegrationTest test
```

## Configuration

Key properties (see `src/main/resources/application.yml` and `.env.example`):

| Property | Default | Purpose |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/inventory_db` / `inventory` / `inventory` | Database connection |
| `JWT_SECRET` | `change-me-please` | HS256 signing secret. **Startup fails under prod/production profiles if unset or left at the default** |
| `jwt.use-rs256` | `false` | Switch to RS256 with PEM keys (`docs/frontend-req/rs256.md`) |
| `cors.allowed-origins` | comma-separated list | Allowed browser origins |
| `jwt.access-token-expiration-seconds` / `jwt.refresh-token-expiration-seconds` | `900` / `604800` | Token lifetimes |
| `security.login.max-attempts` / `security.login.window-seconds` | `5` / `60` | Login lockout threshold and window (per username) |

## Project structure

- `src/main/java/com/example/inventory/domain` — domain entities, value objects, repository interfaces
- `src/main/java/com/example/inventory/application` — commands, queries, handlers (use cases), ports (e.g., `PaymentFailureNotifier`)
- `src/main/java/com/example/inventory/infrastructure` — JPA persistence with Envers auditing, security (JWT, rate limiting, secret guard), WebSocket, scheduled jobs
- `src/main/java/com/example/inventory/web` — REST controllers, DTOs, exception handling
- `src/test/java` — unit and integration tests (72 tests)
- `contracts/` — **git submodule** shared with the frontend repo: `api/openapi.yaml` (OpenAPI 3 contract — regenerate via `mvn test -Dtest=OpenApiContractExportTest` after any API change, then commit inside the submodule and bump the pointer), `ws/asyncapi-ws.md` (WebSocket/STOMP destinations, auth, push payloads), `prd/frontend-prd.md` (frontend product requirements)
- `docs/prd-compliance-gap-report.md` — PRD compliance status; all identified gaps are resolved
- `AGENTS.md` — binding rules for AI agents/humans implementing features here (workflow, architecture, contract-first, testing conventions)
- `docs/development-pitfalls.md` — build/testing gotchas and project conventions; read before extending the codebase
- `docs/frontend-req/` — frontend requirements contract and preparation plan

## Notes

- The `dev` profile auto-creates/updates the database schema (`ddl-auto: update`).
- Error responses always follow the same JSON shape so clients can centralize handling.
- For production: set a strong `JWT_SECRET` (the app refuses to start without one under prod profiles, or enable RS256), restrict `cors.allowed-origins`, and use PostgreSQL rather than H2.
- Login attempts are rate-limited per username: after 5 failures within 60s (configurable), even correct passwords return `429` until the window expires.
- Product and order changes are audited via Hibernate Envers (`revinfo`, `*_aud` tables); expose history through `/api/admin/audit/*`.
