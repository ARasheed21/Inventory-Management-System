# PRD Compliance Gap Report

## Scope
This report compares the PRD user stories and contract expectations in [docs/prd.md](docs/prd.md) with the current code and test evidence in the repository.

> Last updated: 2026-08-24. Gap numbers are kept stable across revisions for traceability; solved gaps move to the "Resolved" section with their resolution evidence.

## Verification Evidence
The current repository was re-verified with Maven test evidence from the latest run:
- `mvn test` -> `Tests run: 66, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

That result is consistent with the current controller, domain, persistence, security, registration, and benchmark proof points.

## Status Summary

### Resolved gaps

#### Gap 1. Admin inventory CRUD — RESOLVED
PRD stories: 1, 2, 3, 4, 33

Resolution evidence:
- [InventoryController.java](../src/main/java/com/example/inventory/web/controllers/InventoryController.java) exposes real `POST /api/inventory/products` (201) and `PUT /api/inventory/products/{id}` (200), admin-only via `@PreAuthorize("hasRole('ADMIN')")`.
- Backed by real write flows: [CreateProductCommandHandler.java](../src/main/java/com/example/inventory/application/handlers/CreateProductCommandHandler.java) and [UpdateProductCommandHandler.java](../src/main/java/com/example/inventory/application/handlers/UpdateProductCommandHandler.java) persist through `ProductRepository`.
- Verified by [InventoryControllerIntegrationTest.java](../src/test/java/com/example/inventory/web/InventoryControllerIntegrationTest.java).

Remaining sub-item (tracked under Gap 10): stock-adjustment auditing is not yet wired for these writes.

#### Gap 2. Product browsing and product detail — RESOLVED
PRD stories: 5, 6

Resolution evidence:
- `GET /api/products` returns a paginated, searchable catalog page (search across name/description, category filter, page/size) via [ListProductsQueryHandler.java](../src/main/java/com/example/inventory/application/handlers/ListProductsQueryHandler.java).
- `GET /api/products/{id}` returns product detail via [GetProductQueryHandler.java](../src/main/java/com/example/inventory/application/handlers/GetProductQueryHandler.java).
- Both endpoints live in [InventoryController.java](../src/main/java/com/example/inventory/web/controllers/InventoryController.java) and are covered by [InventoryControllerIntegrationTest.java](../src/test/java/com/example/inventory/web/InventoryControllerIntegrationTest.java).

#### Gap 3. Shopping-cart workflow — RESOLVED
PRD story: 7

Resolution evidence:
- Cart is a first-class persisted feature: `cart_items` table, [CartRepository.java](../src/main/java/com/example/inventory/domain/repositories/CartRepository.java), [JpaCartRepository.java](../src/main/java/com/example/inventory/infrastructure/persistence/jpa/JpaCartRepository.java), and [CartHandler.java](../src/main/java/com/example/inventory/application/handlers/CartHandler.java).
- Full cart API in [CartController.java](../src/main/java/com/example/inventory/web/controllers/CartController.java): get (`GET /api/cart`), add (`POST /api/cart`), update (`PUT /api/cart/{itemId}`), remove (`DELETE /api/cart/{itemId}`), scoped to the authenticated principal.
- Verified by [CartControllerIntegrationTest.java](../src/test/java/com/example/inventory/web/CartControllerIntegrationTest.java).

Note: placing an order still accepts a raw item list rather than orchestrating from cart contents; treat "place-order-from-cart" as a small follow-up if the PRD requires it strictly.

#### Gap 8. Security identity model — MOSTLY RESOLVED (persistent accounts + registration done; Keycloak not integrated)
PRD stories: 23, 24, 25

Resolution evidence:
- Accounts are now persisted in a database `accounts` table ([AccountJpaEntity.java](../src/main/java/com/example/inventory/infrastructure/persistence/jpa/AccountJpaEntity.java)) with unique username/email and BCrypt password hashes — no more in-memory user catalog.
- Customer self-registration exists: `POST /auth/register` in [AuthController.java](../src/main/java/com/example/inventory/web/controllers/AuthController.java) returns tokens on signup, rejects duplicates with `409`, invalid payloads with `400`.
- Login/refresh/`/auth/me` flows load users from the database ([UserRegistry.java](../src/main/java/com/example/inventory/infrastructure/security/UserRegistry.java)); seeded admin/warehouse/customer accounts survive restarts.
- Roles are loaded from the database and drive authorization end-to-end (JWT claim -> [JwtAuthenticationConverter.java](../src/main/java/com/example/inventory/infrastructure/security/JwtAuthenticationConverter.java) -> method/rule security); registered CUSTOMER tokens are rejected on admin endpoints.
- All behaviors verified by [RegistrationIntegrationTest.java](../src/test/java/com/example/inventory/web/RegistrationIntegrationTest.java) (register->login->access, duplicate rejection, validation, role-based access, garbage-token rejection).

Remaining sub-items:
1. Keycloak issuer validation is not configured; tokens are self-issued HS256/RS256 JWTs.
2. Production profile should enforce a non-default `jwt.secret` (default `change-me-please` remains forgeable if deployed unset).

#### Gap 4. Countdown timer / reservation UX — RESOLVED
PRD story: 10

Resolution evidence:
- Every order response now carries a server-computed `reservationSecondsRemaining` field (0 when the order is not PENDING), immune to client clock skew; computed in [OrderMapper.java](../src/main/java/com/example/inventory/web/mapper/OrderMapper.java) and exposed by get/list/create/payment/cancel/ship/deliver endpoints via [OrderResponse.java](../src/main/java/com/example/inventory/web/dto/OrderResponse.java).
- `GET /api/orders/status/{orderId}` also returns `reservationSecondsRemaining` for lightweight polling.
- The reservation timeout job ([ReservationTimeoutJob.java](../src/main/java/com/example/inventory/infrastructure/jobs/ReservationTimeoutJob.java)) now publishes a `{orderId, status: RESERVATION_EXPIRED}` message to each affected customer's `/user/queue/orders` WebSocket queue as it cancels their expired orders, so an open checkout form can be invalidated in real time.
- All behaviors verified by [ReservationCountdownIntegrationTest.java](../src/test/java/com/example/inventory/web/ReservationCountdownIntegrationTest.java) (pending countdown in (0,900], paid order reports 0, status-endpoint countdown, real STOMP session receives the expiry push).

#### Gap 6. Order status update / warehouse fulfillment API — RESOLVED
PRD stories: 17, 18, 19, 31

Resolution evidence:
- `POST /api/orders/{id}/ship` and `POST /api/orders/{id}/deliver` are now exposed in [OrderController.java](../src/main/java/com/example/inventory/web/controllers/OrderController.java), wired to the existing [ShipOrderCommandHandler](../src/main/java/com/example/inventory/application/handlers/ShipOrderCommandHandler.java) and [DeliverOrderCommandHandler](../src/main/java/com/example/inventory/application/handlers/DeliverOrderCommandHandler.java).
- Both endpoints require WAREHOUSE or ADMIN roles (`@PreAuthorize`); customers receive `403`, anonymous callers `401`.
- Invalid lifecycle transitions return `409 Conflict`; unknown order ids return `404`.
- Each transition publishes an order update over WebSocket so connected frontends observe SHIPPED/DELIVERED in real time.
- All behaviors verified by [FulfillmentApiIntegrationTest.java](../src/test/java/com/example/inventory/web/FulfillmentApiIntegrationTest.java) (ship->deliver lifecycle, role enforcement, invalid transitions, unknown order).

#### Gap 5. Payment failure notification — RESOLVED
PRD story: 12

Resolution evidence:
- New application-layer port [PaymentFailureNotifier.java](../src/main/java/com/example/inventory/application/ports/PaymentFailureNotifier.java) with a WebSocket adapter ([WebSocketPaymentFailureNotifier.java](../src/main/java/com/example/inventory/infrastructure/websocket/WebSocketPaymentFailureNotifier.java)) — the handler depends on the port only, per the hexagonal constitution.
- Paying an order whose reservation has expired now fails (`409 Conflict`) instead of silently succeeding: [ProcessPaymentCommandHandler.java](../src/main/java/com/example/inventory/application/handlers/ProcessPaymentCommandHandler.java) guards with `Order.isReservationExpired`, publishes `{orderId, status: PAYMENT_FAILED, reason}` to the customer's `/user/queue/orders`, then rethrows.
- Invalid payable-state transitions (already paid/cancelled) follow the same notify-then-fail path.
- Unknown-order payment attempts return `404` (aligned with ship/deliver handlers).
- All behaviors verified by [PaymentFailureNotificationIntegrationTest.java](../src/test/java/com/example/inventory/web/PaymentFailureNotificationIntegrationTest.java) (expired-order push + 409, no false notification on successful payment, unknown order 404).

#### Gap 7. Audit-history read API — RESOLVED
PRD stories: 20, 21

Resolution evidence:
- New admin-only endpoints `GET /api/admin/audit/products/{id}` and `GET /api/admin/audit/orders/{id}` in [AuditHistoryController.java](../src/main/java/com/example/inventory/web/AuditHistoryController.java) return Envers revision lists (`{revision, timestamp, author, revisionType, snapshot}`) resolved by external ids.
- Customers and warehouse users receive `403`; only ADMIN can read history.
- Verified by [AuditHistoryIntegrationTest.java](../src/test/java/com/example/inventory/web/AuditHistoryIntegrationTest.java): product history shows ADD then MOD after an update; order history tracks PENDING -> PAID -> SHIPPED snapshots.

#### Gap 10. Inventory adjustment auditing — RESOLVED
PRD stories: 33

Resolution evidence:
- [ProductJpaEntity.java](../src/main/java/com/example/inventory/infrastructure/persistence/jpa/ProductJpaEntity.java) is now `@Audited`, so admin product/stock writes produce a full Envers revision trail alongside order auditing.
- `products_aud` table added to both H2 schemas.
- The audit trail is observable end-to-end through the Gap 7 read endpoint (product create + update yields ADD/MOD revisions), verified by the same integration test.

#### Gap 9. Reserved inventory visibility — RESOLVED
PRD story: 29

Resolution evidence:
- New WAREHOUSE/ADMIN endpoint `GET /api/inventory/reserved` returns per-product `{productId, name, quantityInStock, quantityReserved, quantityAvailable}`, where reserved quantities are aggregated from PENDING orders ([GetReservedInventoryQueryHandler.java](../src/main/java/com/example/inventory/application/handlers/GetReservedInventoryQueryHandler.java), [OrderRepository.findReservedQuantitiesByProduct](../src/main/java/com/example/inventory/domain/repositories/OrderRepository.java)).
- Reserved amounts drop as soon as an order leaves PENDING (paid/cancelled/expired).
- Verified by [ReservedInventoryIntegrationTest.java](../src/test/java/com/example/inventory/web/ReservedInventoryIntegrationTest.java) (reserved=3/available=7 for a seeded pending order, drop to 0 after payment, role enforcement).

### Still missing

#### Gap 8 remainder — Keycloak / production JWT hardening
PRD stories: 23, 24, 25 (partial)

Evidence:
1. Keycloak issuer validation is not configured; tokens are self-issued HS256/RS256 JWTs.
2. Production profile should enforce a non-default `jwt.secret` (default `change-me-please` remains forgeable if deployed unset).

## Bottom Line
The repository now satisfies the PRD's core customer-facing catalog, cart, admin inventory write, persistent account/registration, warehouse fulfillment, reservation countdown/notification, payment-failure notification, audit trail/history, and reserved-inventory reporting flows, with a green 66-test suite. The only remaining PRD item is the true Keycloak integration and production JWT secret hardening (Gap 8 remainder).

## Recommended delivery order
1. Enforce non-default JWT secret in prod + optional config-gated Keycloak issuer validation (Gap 8 remainder).
