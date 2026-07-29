# Implementation Plan for Spec 1

This plan breaks Spec 1 into concrete implementation phases while staying aligned with the project constitution.

## Constitutional Guardrails
- The domain layer must remain framework-free.
- No Spring, JPA, or Hibernate imports are allowed in the domain layer.
- All state changes on an Order must go through the aggregate root.
- Domain logic must be covered by unit tests.
- The implementation must be verifiable with Maven and JaCoCo.

---

## Phase 1 — Create the domain foundation

### Goal
Establish the package structure and the basic immutable value objects needed by the domain.

### Tasks
1. Create the package structure under the domain package:
   - com.example.inventory.domain
   - com.example.inventory.domain.exceptions
   - com.example.inventory.domain.events
2. Implement the following value objects as immutable plain Java classes or records:
   - Money
   - Address
   - SKU
3. Implement the OrderStatus enum with the transition logic from the PRD.
4. Create the core exception classes:
   - DomainException
   - InvalidOrderStateException
   - InsufficientInventoryException
5. Add unit tests for each value object and the enum transition rules.

### Deliverables
- Immutable domain value objects
- State transition rules encapsulated in the enum
- Initial domain exception types
- Unit tests for value objects and state validation

---

## Phase 2 — Implement the Product and Order item model

### Goal
Define the main business entities used by the order aggregate.

### Tasks
1. Implement Product as a domain entity with:
   - id
   - name
   - description
   - price
   - quantityInStock
   - version
2. Implement OrderItem with:
   - productId
   - quantity
   - unitPrice
   - sku
3. Add validation rules for:
   - non-blank product names
   - non-negative prices
   - positive quantities
   - non-negative stock values
4. Add unit tests for product and order-item validation.

### Deliverables
- Product entity with domain validation
- OrderItem entity with quantity and price rules
- Tests covering invalid inputs

---

## Phase 3 — Implement the Order aggregate root

### Goal
Build the aggregate root that owns all order state transitions and reservation rules.

### Tasks
1. Implement the Order aggregate root with:
   - id
   - customerId
   - items
   - totalAmount
   - status
   - createdAt
   - updatedAt
   - reservedUntil
   - version
2. Implement the lifecycle methods:
   - placeOrder(...)
   - pay()
   - cancel()
   - ship()
   - deliver()
3. Enforce the state machine rules inside the aggregate.
4. Implement reservation expiry logic through isReservationExpired(Instant now).
5. Record domain events such as:
   - OrderPlacedEvent
   - OrderCancelledEvent
   - OrderPaymentCompletedEvent
6. Ensure all state mutations happen through these aggregate methods only.

### Deliverables
- Fully functional Order aggregate
- Reservation timeout behavior based on the aggregate
- Domain events emitted from the aggregate
- Unit tests for all lifecycle transitions

---

## Phase 4 — Define repository contracts in the domain layer

### Goal
Expose persistence abstractions without leaking infrastructure concerns into the domain.

### Tasks
1. Define the OrderRepository interface in the domain layer.
2. Define the ProductRepository interface in the domain layer.
3. Keep these interfaces framework-free and repository-focused.
4. Add lightweight tests or contract-based tests using fake implementations to verify handler interaction later.

### Deliverables
- Domain repository interfaces
- No infrastructure imports in the domain package
- Repository interfaces ready for implementation in later specs

---

## Phase 5 — Harden and verify the domain layer

### Goal
Ensure the implementation meets the constitution and quality gate expectations.

### Tasks
1. Add unit tests for:
   - pending to paid transition
   - pending to cancelled transition
   - paid to shipped transition
   - shipped to delivered transition
   - invalid transitions
   - reservation expiry
2. Run the Maven test suite.
3. Run JaCoCo for coverage reporting.
4. Review the domain package for any accidental Spring or JPA imports.
5. Confirm that all public domain methods are self-documenting and that no setter-based mutation is used.

### Deliverables
- Passing test suite
- Domain coverage meeting the project target
- Clean domain architecture that satisfies the constitution

---

## Definition of Done for Spec 1
- The domain layer contains no Spring or persistence annotations.
- The Order aggregate owns all order state transitions.
- The reservation timeout logic is implemented in the domain.
- Unit tests cover the core behavior and state machine.
- Maven tests and JaCoCo reporting pass successfully.
