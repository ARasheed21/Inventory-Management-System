# Spec 6: Web Interface (REST API) & OpenAPI Documentation

## 1. Spec Metadata
- **Name**: Web Interface (REST API) & OpenAPI Documentation
- **Dependencies**: Spec 2, Spec 5
- **Estimated Effort**: High

## 2. In-Scope Artifacts
After this spec is complete, the following artifacts should exist:

- Package: `com.example.inventory.web.controllers`
  - `OrderController`
  - `InventoryController`

- Package: `com.example.inventory.web.dto`
  - `CreateOrderRequest`
  - `CreateOrderItemRequest`
  - `OrderResponse`
  - `InventoryResponse`

- Package: `com.example.inventory.web.mapper`
  - `OrderMapper`

## 3. Core Domain Models & Contracts

### Controllers
- `OrderController`
  - endpoints: `POST /api/orders`, `GET /api/orders/{id}`, `GET /api/orders`, `POST /api/orders/{id}/payment`, `POST /api/orders/{id}/cancel`
- `InventoryController`
  - endpoints: `GET /api/inventory`, `POST /api/inventory/products`, `PUT /api/inventory/products/{id}`

### DTOs
- Request/response payloads must reflect the application-layer commands and queries.
- Swagger/OpenAPI annotations must be added to every endpoint and DTO field.

## 4. Behavioral Specifications
1. Web controllers delegate to application handlers.
2. DTOs are mapped to application commands and queries.
3. API errors are translated into consistent error responses.

## 5. Input / Output Contracts
- Example request payloads should be documented clearly.
- Response payloads should be consistent and predictable.

## 6. Technical Constraints / Non-Functional Rules
- All endpoints must be annotated with OpenAPI metadata.
- The generated API docs must be available at `/v3/api-docs` and `/swagger-ui.html`.
- The web layer must depend on the application layer, not on the domain implementation directly.

## 7. Acceptance Criteria & Test Matrix
1. Given a valid order creation request, when the endpoint is called, then a 201/200 response is returned.
2. Given an invalid request, when the endpoint is called, then a validation error is returned.
3. Given a request to the Swagger endpoint, when the docs are requested, then the OpenAPI document is served.
