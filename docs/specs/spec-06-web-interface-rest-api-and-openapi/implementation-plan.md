# Implementation Plan for Spec 6

This plan breaks Spec 6 into concrete implementation phases so the REST API and OpenAPI work can be delivered incrementally while staying aligned with the repository’s current Spring Boot and CQRS structure.

## Constitutional Guardrails
- The web layer may depend on the application layer, but it must not couple directly to the domain implementation.
- Controllers must delegate business behavior to application handlers or query handlers.
- Request/response DTOs belong in the web transport layer and should be documented explicitly for OpenAPI generation.
- Validation, error mapping, and API documentation must be testable and consistent across endpoints.

---

## Phase 1 — Establish the web transport contract

### Goal
Create the transport-layer DTOs and request/response contracts that the controllers will expose.

### Tasks
1. Create the web-layer package structure under `com.example.inventory.web.controllers`, `com.example.inventory.web.dto`, and `com.example.inventory.web.mapper`.
2. Add transport request DTOs for order creation and inventory writes, including `CreateOrderRequest`, `CreateOrderItemRequest`, and any needed validation annotations.
3. Add response DTOs for orders and inventory, including `OrderResponse` and `InventoryResponse`, with fields that match the API contract.
4. Annotate DTO fields with OpenAPI schema metadata such as descriptions, required flags, and examples.
5. Keep these DTOs focused on API transport rather than domain persistence or behavior.

### Deliverables
- Web DTO package with documented request/response contracts
- Validation-ready transport types
- OpenAPI-friendly schema objects

---

## Phase 2 — Add controller entry points

### Goal
Expose the REST API endpoints described in the spec through dedicated controllers.

### Tasks
1. Implement `OrderController` under `com.example.inventory.web.controllers`.
2. Add endpoints for:
   - `POST /api/orders`
   - `GET /api/orders/{id}`
   - `GET /api/orders`
   - `POST /api/orders/{id}/payment`
   - `POST /api/orders/{id}/cancel`
3. Implement `InventoryController` under `com.example.inventory.web.controllers`.
4. Add endpoints for:
   - `GET /api/inventory`
   - `POST /api/inventory/products`
   - `PUT /api/inventory/products/{id}`
5. Ensure every endpoint delegates to the corresponding application handler or query handler through the existing application layer.
6. Return correct HTTP status codes, especially for creation (`201`) and validation failures (`400`/`422`).

### Deliverables
- `OrderController`
- `InventoryController`
- Publicly accessible REST entry points aligned with the spec contract

---

## Phase 3 — Add mapping and command/query translation

### Goal
Bridge transport DTOs to application-layer commands and queries without leaking domain concerns into the web layer.

### Tasks
1. Create `OrderMapper` in the web mapper package.
2. Map `CreateOrderRequest` into the application-level `PlaceOrderCommand` or equivalent command shape.
3. Map controller response DTOs into the application `OrderResponse` contract so the web layer can return stable payloads.
4. Provide a single translation path for order lookup, payment, cancellation, and list operations.
5. Keep all mapping responsibilities isolated in the mapper so controllers stay thin.

### Deliverables
- `OrderMapper`
- Stable translation between transport and application DTOs
- Thin controller implementation with explicit delegation boundaries

---

## Phase 4 — Add consistent web error handling

### Goal
Convert application errors into predictable, documented API responses.

### Tasks
1. Create a global exception handler for invalid input, missing entities, and business rule failures.
2. Translate handler exceptions into structured JSON responses with meaningful error messages and status codes.
3. Ensure validation failures return clear 400-style payloads for malformed requests.
4. Confirm that business-rule violations are surfaced as API-safe errors rather than leaking stack traces.
5. Keep the response format consistent across all controllers.

### Deliverables
- Error-response strategy for the web API
- Consistent API failure contract across order and inventory endpoints

---

## Phase 5 — Add OpenAPI metadata and documentation exposure

### Goal
Ensure the REST API is fully described in the generated OpenAPI document.

### Tasks
1. Add `@Operation`, `@ApiResponses`, and `@Parameter` annotations to controller methods as needed.
2. Annotate request/response DTO fields with Swagger/OpenAPI schema metadata to support generated docs.
3. Confirm the project exposes the generated docs at `/v3/api-docs` and `/swagger-ui.html`.
4. Verify that request examples and response examples are documented clearly enough for manual API exploration.
5. Ensure the documentation reflects the actual controller contract, not a stale or inferred design.

### Deliverables
- OpenAPI-ready controllers and DTOs
- Swagger UI and OpenAPI endpoint availability
- Fully documented REST surface for the external API

---

## Phase 6 — Add API-focused tests and verification

### Goal
Verify the REST contract described in the acceptance criteria.

### Tasks
1. Add controller-level integration tests for successful order creation and lookup flows.
2. Add validation tests to prove invalid payloads return a validation error response.
3. Add a docs smoke test proving the OpenAPI document is reachable from the configured Swagger endpoints.
4. Keep tests focused on HTTP contract behavior rather than internal domain mechanics.
5. Run the Maven test suite and confirm the new web/API coverage passes with the existing build.

### Deliverables
- API integration coverage for the web contract
- Validation and documentation smoke tests
- Verified REST and OpenAPI implementation status

---

## Definition of Done for Spec 6
- `OrderController` and `InventoryController` exist in the web layer and expose the required endpoints.
- Web DTOs and mapper classes are present in the expected packages.
- Controllers delegate to application handlers rather than implementing business logic directly.
- OpenAPI annotations are present across endpoints and payload types.
- Validation and error responses are consistent and documented.
- Maven tests pass, including the new API and documentation coverage.
