# Spec 1: Domain Model & Core Business Logic

## 1. Spec Metadata
- **Name**: Domain Model & Core Business Logic
- **Dependencies**: None
- **Estimated Effort**: High

## 2. In-Scope Artifacts
After this spec is complete, the following Java artifacts should exist:

- Package: `com.example.inventory.domain`
  - `Order` (aggregate root)
  - `OrderStatus` (enum)
  - `OrderItem`
  - `Product`
  - `Customer`
  - `Money`
  - `Address`
  - `SKU`
  - `OrderPlacedEvent`
  - `OrderCancelledEvent`
  - `OrderPaymentCompletedEvent`
  - `OrderRepository` (interface)
  - `ProductRepository` (interface)
  - `OrderDomainService` (optional, if needed for orchestration)

- Package: `com.example.inventory.domain.exceptions`
  - `DomainException`
  - `InvalidOrderStateException`
  - `InsufficientInventoryException`

## 3. Core Domain Models & Contracts

### Order (Aggregate Root)
Attributes:
- `String id`
- `String customerId`
- `List<OrderItem> items`
- `Money totalAmount`
- `OrderStatus status`
- `Instant createdAt`
- `Instant updatedAt`
- `Instant reservedUntil`
- `long version`

Methods:
- `Order placeOrder(String customerId, List<OrderItem> items, Instant createdAt)`
- `void pay()`
- `void cancel()`
- `void ship()`
- `void deliver()`
- `boolean isReservationExpired(Instant now)`
- `boolean canTransitionTo(OrderStatus targetStatus)`
- `List<DomainEvent> getDomainEvents()`

Business invariants:
- An order can only transition through the allowed lifecycle: `PENDING -> PAID -> SHIPPED -> DELIVERED` and `PENDING -> CANCELLED` or `PAID -> CANCELLED`.
- A pending order must have a reservation expiry time set when created.
- A paid order cannot be cancelled unless the domain explicitly allows it through the aggregate rule.
- An order cannot be shipped unless it is already paid.
- An order cannot be delivered unless it is already shipped.

### OrderStatus
Values:
- `PENDING`
- `PAID`
- `SHIPPED`
- `DELIVERED`
- `CANCELLED`

Rules:
- `canTransitionTo()` must enforce the state machine from the PRD.

### OrderItem
Attributes:
- `String productId`
- `int quantity`
- `Money unitPrice`
- `SKU sku`

Rules:
- Quantity must be greater than zero.
- Unit price must be non-negative.

### Product
Attributes:
- `String id`
- `String name`
- `String description`
- `Money price`
- `int quantityInStock`
- `long version`

Rules:
- Product name must not be blank.
- Price must be non-negative.
- Quantity in stock must not be negative.

### Customer
Attributes:
- `String id`
- `String name`
- `String email`
- `Address address`

### Money
Attributes:
- `BigDecimal amount`
- `String currency`

Rules:
- Must be immutable.
- Must validate currency and non-negative amount.

### Address
Attributes:
- `String street`
- `String city`
- `String state`
- `String postalCode`
- `String country`

Rules:
- Must validate required fields.

### SKU
Attributes:
- `String value`

Rules:
- Must be non-blank and uppercase format is preferred.

### Domain Events
- `OrderPlacedEvent`
  - fields: `String orderId`, `Instant reservedUntil`, `String customerId`
- `OrderCancelledEvent`
  - fields: `String orderId`, `String reason`
- `OrderPaymentCompletedEvent`
  - fields: `String orderId`

### Repositories
- `OrderRepository`
  - methods: `save(Order order)`, `findById(String id)`, `findAll()`, `findByCustomerId(String customerId)`
- `ProductRepository`
  - methods: `save(Product product)`, `findById(String id)`, `findAll()`

## 4. Behavioral Specifications
The main flow for this spec is the lifecycle of a customer order:

1. A customer creates an order with one or more items.
2. The aggregate validates that the request is structurally valid.
3. The aggregate sets the order status to `PENDING` and calculates the reservation expiry at `createdAt + 15 minutes`.
4. The aggregate records a domain event indicating that the order was placed.
5. The aggregate can later transition to `PAID`, `CANCELLED`, `SHIPPED`, or `DELIVERED` based on strict rules.
6. The domain must expose `isReservationExpired(Instant now)` so a scheduler can later decide whether to cancel the order.

## 5. Input / Output Contracts
This spec is domain-focused and should not expose web DTOs. The contracts are internal domain contracts only.

- Commands are not created in this spec; they belong to Spec 2.
- The repository interfaces are pure Java contracts and should not depend on Spring or JPA.

## 6. Technical Constraints / Non-Functional Rules
- No Spring annotations are allowed in the domain layer.
- No JPA, Hibernate, or database imports are allowed in the domain layer.
- Value objects must be immutable.
- All state changes must happen through aggregate methods rather than setters.
- Use plain Java classes and enums only.

## 7. Acceptance Criteria & Test Matrix
1. Given a valid order with items and a customer, when the order is placed, then the status is `PENDING` and the reservation expiry is set 15 minutes in the future.
2. Given a pending order, when it is paid, then the status becomes `PAID`.
3. Given a paid order, when it is shipped, then the status becomes `SHIPPED`.
4. Given a shipped order, when it is delivered, then the status becomes `DELIVERED`.
5. Given a pending order, when it is cancelled, then the status becomes `CANCELLED`.
6. Given an order whose reservation expiry has passed, when `isReservationExpired()` is checked, then it returns `true`.
7. Given an invalid transition, when the aggregate method is invoked, then an exception is thrown.
8. Given a product with insufficient stock, when inventory is validated, then an appropriate domain exception is raised.
9. Given a valid money value, when it is created, then it is immutable and preserves currency semantics.
10. Given a repository interface, when used by application services, then it remains free of infrastructure dependencies.

## 8. Implementation Plan
A phased execution plan for this spec is available at [implementation-plan.md](implementation-plan.md).
