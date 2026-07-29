# Spec 2: Application Layer (CQRS Handlers)

## 1. Spec Metadata
- **Name**: Application Layer (CQRS Handlers)
- **Dependencies**: Spec 1
- **Estimated Effort**: High

## 2. In-Scope Artifacts
After this spec is complete, the following artifacts should exist:

- Package: `com.example.inventory.application.commands`
  - `PlaceOrderCommand`
  - `CancelOrderCommand`
  - `ProcessPaymentCommand`
  - `ShipOrderCommand`
  - `DeliverOrderCommand`

- Package: `com.example.inventory.application.queries`
  - `GetOrderQuery`
  - `GetInventoryQuery`
  - `ListOrdersQuery`

- Package: `com.example.inventory.application.handlers`
  - `PlaceOrderCommandHandler`
  - `CancelOrderCommandHandler`
  - `ProcessPaymentCommandHandler`
  - `ShipOrderCommandHandler`
  - `DeliverOrderCommandHandler`
  - `GetOrderQueryHandler`
  - `GetInventoryQueryHandler`
  - `ListOrdersQueryHandler`

- Package: `com.example.inventory.application.dto`
  - `OrderResponse`
  - `InventoryItemResponse`
  - `CreateOrderRequest`

## 3. Core Domain Models & Contracts

### Commands
- `PlaceOrderCommand`
  - fields: `String customerId`, `List<OrderItemRequest> items`
  - responsibility: move the order creation flow from the web layer into the application layer

- `CancelOrderCommand`
  - fields: `String orderId`

- `ProcessPaymentCommand`
  - fields: `String orderId`

- `ShipOrderCommand`
  - fields: `String orderId`

- `DeliverOrderCommand`
  - fields: `String orderId`

### Queries
- `GetOrderQuery`
  - fields: `String orderId`

- `GetInventoryQuery`
  - fields: none

- `ListOrdersQuery`
  - fields: `String customerId`, `String status`

### Handlers
Each handler should:
- receive the command/query object
- use the repository interfaces from the domain layer
- orchestrate domain methods
- return an output object or a projection

The application layer must not contain business logic beyond orchestration and validation of the command/query shape.

## 4. Behavioral Specifications
The application layer is responsible for the use case flow:

1. `PlaceOrderCommandHandler` loads the product catalog, validates stock, creates the order through the domain aggregate, and persists the state through the repository.
2. `ProcessPaymentCommandHandler` loads the order, calls the aggregate’s payment transition, and saves the updated state.
3. `CancelOrderCommandHandler` invokes the domain cancellation rule and persists the result.
4. `ShipOrderCommandHandler` and `DeliverOrderCommandHandler` change the state through the aggregate.
5. Query handlers should return read models that are independent of the domain entities.

## 5. Input / Output Contracts
### Request DTOs
- `CreateOrderRequest`
  - fields: `String customerId`, `List<CreateOrderItemRequest> items`

- `CreateOrderItemRequest`
  - fields: `String productId`, `int quantity`

### Response DTOs
- `OrderResponse`
  - fields: `String id`, `String customerId`, `String status`, `BigDecimal totalAmount`, `String currency`, `Instant createdAt`, `Instant reservedUntil`

- `InventoryItemResponse`
  - fields: `String productId`, `String name`, `int quantityInStock`

## 6. Technical Constraints / Non-Functional Rules
- The application layer may depend on the domain layer, but it must not depend on Spring persistence or web frameworks.
- Handlers must be stateless and reusable.
- Commands and queries should be simple immutable value objects.
- No direct database or HTTP concerns should exist here.

## 7. Acceptance Criteria & Test Matrix
1. Given a valid place-order command, when the handler runs, then an order is created in `PENDING` state.
2. Given a payment command for a pending order, when the handler runs, then the order becomes `PAID`.
3. Given a cancel command for a pending order, when the handler runs, then the order becomes `CANCELLED`.
4. Given a query for an order, when the handler runs, then it returns a read model instead of a domain entity.
5. Given an inventory query, when the handler runs, then it returns the current stock projection.
6. Given invalid input, when the handler runs, then a domain or application exception is raised.
