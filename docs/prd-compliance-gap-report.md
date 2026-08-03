# PRD Compliance Gap Report

## Scope
This report compares the PRD user stories and contract expectations in [docs/prd.md](docs/prd.md) with the current code and test evidence in the repository.

## Verification Evidence
The current repository was re-verified with Maven test evidence from the latest run:
- `mvn test` -> `Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

That result is consistent with the current controller, domain, persistence, security, and benchmark proof points.

## Status Summary

### Implemented and verified
The following PRD themes are present in the codebase and are covered by tests or exposed contracts:

- Order lifecycle state machine and reservation window are implemented in [src/main/java/com/example/inventory/domain/entities/Order.java](src/main/java/com/example/inventory/domain/entities/Order.java) and exercised by [src/test/java/com/example/inventory/domain/OrderTest.java](src/test/java/com/example/inventory/domain/OrderTest.java).
- CQRS command/query handlers exist for place, pay, cancel, ship, deliver, get, and list flows under [src/main/java/com/example/inventory/application/handlers](src/main/java/com/example/inventory/application/handlers).
- JPA persistence and optimistic locking are present in [src/main/java/com/example/inventory/infrastructure/persistence/jpa/OrderJpaEntity.java](src/main/java/com/example/inventory/infrastructure/persistence/jpa/OrderJpaEntity.java) and [src/main/java/com/example/inventory/infrastructure/persistence/jpa/ProductJpaEntity.java](src/main/java/com/example/inventory/infrastructure/persistence/jpa/ProductJpaEntity.java).
- Envers audit support and revision metadata are configured in [src/main/java/com/example/inventory/infrastructure/audit/EnversConfig.java](src/main/java/com/example/inventory/infrastructure/audit/EnversConfig.java) and [src/main/java/com/example/inventory/infrastructure/audit/AuditRevisionEntity.java](src/main/java/com/example/inventory/infrastructure/audit/AuditRevisionEntity.java).
- Reservation timeout automation is implemented in [src/main/java/com/example/inventory/infrastructure/jobs/ReservationTimeoutJob.java](src/main/java/com/example/inventory/infrastructure/jobs/ReservationTimeoutJob.java) and benchmarked by [src/test/java/com/example/inventory/infrastructure/benchmark/ReservationTimeoutBenchmarkTest.java](src/test/java/com/example/inventory/infrastructure/benchmark/ReservationTimeoutBenchmarkTest.java).
- REST order endpoints exist in [src/main/java/com/example/inventory/web/controllers/OrderController.java](src/main/java/com/example/inventory/web/controllers/OrderController.java), and the main success path is verified by [src/test/java/com/example/inventory/web/OrderControllerIntegrationTest.java](src/test/java/com/example/inventory/web/OrderControllerIntegrationTest.java).
- Inventory read access is exposed through [src/main/java/com/example/inventory/web/controllers/InventoryController.java](src/main/java/com/example/inventory/web/controllers/InventoryController.java).
- Basic role-level access control is present in [src/main/java/com/example/inventory/infrastructure/security/SecurityConfig.java](src/main/java/com/example/inventory/infrastructure/security/SecurityConfig.java) and verified by [src/test/java/com/example/inventory/infrastructure/security/SecurityAccessIntegrationTest.java](src/test/java/com/example/inventory/infrastructure/security/SecurityAccessIntegrationTest.java).

### Missing or only placeholder-level
The following PRD requirements are not currently fulfilled by the repository, even though the PRD explicitly lists them as must-haves.

#### 1. Admin inventory CRUD is not implemented
PRD stories: 1, 2, 3, 4, 33

Evidence:
- [src/main/java/com/example/inventory/web/controllers/InventoryController.java](src/main/java/com/example/inventory/web/controllers/InventoryController.java) has `POST /api/inventory/products` and `PUT /api/inventory/products/{id}` but both return `501 NOT_IMPLEMENTED`.
- There is no real application handler or repository write flow that creates or updates `Product` records from the web contract.
- The current inventory API only returns a single inventory snapshot via `GET /api/inventory` and is not a complete admin inventory management contract.

Task backlog:
1. Add domain write flows for product create/update and inventory adjustment.
2. Add application handlers and commands/queries for inventory write use cases.
3. Implement repository persistence for product writes and stock adjustment.
4. Expose the real admin write endpoints and map them to DTOs.
5. Add audit logging for stock changes and a read endpoint for audit history.

#### 2. Product browsing and product detail customer flows are not present
PRD stories: 5, 6

Evidence:
- The repository contains the `Product` domain entity and `ProductRepository` contract, but there is no customer-facing product catalog or product detail controller.
- The only inventory read path is a single aggregate response from `GetInventoryQueryHandler`, not a browseable product list with pricing and availability metadata.

Task backlog:
1. Add `ProductCatalogQuery` and `ProductDetailQuery` use cases.
2. Implement read models for product lists and product detail responses.
3. Add a `ProductController` with browse/detail endpoints.
4. Add tests for browsing and detail retrieval constraints.

#### 3. Shopping-cart workflow is not modeled as a first-class domain or API feature
PRD story: 7

Evidence:
- The order creation request only contains `customerId` and a list of order items; it is not a cart-backed workflow.
- There is no cart aggregate, cart repository, cart query, or cart endpoint contract in the codebase.

Task backlog:
1. Introduce a cart aggregate and cart repository interface.
2. Add cart add/remove/update endpoints.
3. Add place-order orchestration from cart contents rather than a raw item list.
4. Add cart-specific tests and controller coverage.

#### 4. Countdown timer / customer-facing reservation UX is not implemented
PRD story: 10

Evidence:
- The domain stores `reservedUntil`, but there is no web contract or frontend-facing countdown timer API.
- There is no endpoint that returns a remaining-time projection or a UI-ready reservation countdown payload.

Task backlog:
1. Add a reservation countdown projection query.
2. Return the remaining time in the order response or a dedicated order-status response payload.
3. Add integration coverage for the countdown response contract.

#### 5. Payment failure notification is absent
PRD story: 12

Evidence:
- Payment is modeled as a command handler transition; there is no dedicated failure notification event, handler, or transport path.
- There is no notification infrastructure or API contract for notifying a customer of failed payment.

Task backlog:
1. Add payment failure domain events and notification handlers.
2. Introduce a notification channel abstraction.
3. Add a customer-facing response or event sink for failed payment.
4. Add test coverage for failed-payment messaging and order state.

#### 6. The order status update / warehouse fulfillment API is not exposed
PRD stories: 17, 18, 19, 31

Evidence:
- The domain and command handlers support `ShipOrderCommand` and `DeliverOrderCommand`, but the controller only exposes create, get, list, payment, and cancel.
- There is no controller endpoint such as `PUT /api/orders/{orderId}/status` for the admin/warehouse workflow described in the PRD.
- The order listing query supports filters, but the public API surface does not expose a dedicated admin/warehouse status pipeline contract.

Task backlog:
1. Add `PUT /api/orders/{orderId}/status` with role-aware authorization.
2. Add explicit ship and deliver endpoints or a unified status update contract.
3. Add warehouse/admin query filters for `PAID`, `SHIPPED`, and `DELIVERED` orders.
4. Add controller and security tests for warehouse-role access.

#### 7. The repository currently does not expose a real audit-history read API
PRD stories: 20, 21

Evidence:
- Envers is configured and the revision entity is present, so change tracking is persisted behind the scenes.
- However, the codebase does not expose a web endpoint or read service that returns a usable audit-history view for a given order or product.

Task backlog:
1. Add a query/read model for audit revisions.
2. Add a controller endpoint for retrieving order change history.
3. Add a revision-view DTO and assembly logic.
4. Add admin-only tests for audit retrieval permissions.

#### 8. Security is not aligned with the PRD’s Keycloak JWT resource-server model
PRD stories: 23, 24, 25

Evidence:
- [src/main/java/com/example/inventory/infrastructure/security/SecurityConfig.java](src/main/java/com/example/inventory/infrastructure/security/SecurityConfig.java) uses in-memory `UserDetailsService` with HTTP Basic, not an OAuth2 JWT resource server tied to Keycloak.
- [src/main/java/com/example/inventory/infrastructure/security/JwtAuthenticationConverter.java](src/main/java/com/example/inventory/infrastructure/security/JwtAuthenticationConverter.java) is present as a converter scaffold, but the active production path is still the in-memory Basic-auth configuration.
- There are no customer registration/authentication endpoints or a real identity/account model in the codebase.

Task backlog:
1. Replace the in-memory user catalog with real OAuth2 resource server configuration and Keycloak issuer validation.
2. Add customer registration/authentication contracts and account persistence.
3. Add JWT role mapping and method-level security enforcement that follows the PRD roles.
4. Add security integration tests with mock or container-based JWT tokens.

#### 9. Reserved inventory visibility is not implemented
PRD story: 29

Evidence:
- Reservation timeout logic is implemented, and the order domain carries a reservation window.
- There is no dedicated query/report path that returns “currently reserved inventory” by pending order allocation.

Task backlog:
1. Add a reserved-inventory projection query.
2. Implement a reporting endpoint for allocated stock by product.
3. Add tests for true vs. reserved inventory counts.

#### 10. Inventory adjustment auditing is not fully wired
PRD story: 33

Evidence:
- Envers is configured for order auditing, but the current code does not yet show a full stock-adjustment audit flow for admin inventory changes.
- The admin write endpoints are placeholders, so there is no finished AUDITABLE inventory-write path to validate.

Task backlog:
1. Add inventory write events and audit messages for stock changes.
2. Make product and stock changes auditable through the same Envers pattern.
3. Add admin read APIs for stock-change history.

## Bottom Line
The repository is strongly aligned with the PRD’s domain/core and infrastructure baseline, and the current test suite is green. However, the PRD is not fully satisfied end-to-end because the system still lacks the expected customer-facing product browsing, shopping-cart, real Keycloak/OAuth2 identity flow, admin inventory write contract, warehouse status-management API, notification pipeline, audit-history read endpoint, and reserved-inventory reporting features.

## Recommended delivery order
1. Finish the admin inventory write path and stock-audit trail.
2. Add the real Keycloak JWT resource-server identity story.
3. Add the customer product browse/detail and cart flows.
4. Expose the warehouse status update contract and reserved-inventory queries.
5. Add notification and audit-history endpoints so the current backend matches the PRD’s customer/admin operating model.
