# Implementation Plan for Spec 2

This plan breaks Spec 2 into concrete implementation phases that stay aligned with the project constitution and the existing domain model from Spec 1.

## Constitutional Guardrails
- The application layer may depend on the domain layer, but it must remain free from Spring persistence and web framework concerns.
- Handlers must be stateless and reusable.
- Commands and queries must be simple immutable value objects.
- All business behavior remains in the domain layer; the application layer only orchestrates and validates use cases.
- The implementation must be verifiable via Maven tests.

---

## Phase 1 — Create the application package structure and DTOs

### Goal
Establish the application layer skeleton and define the contracts that the handlers will work with.

### Tasks
1. Create the following packages:
   - com.example.inventory.application.commands
   - com.example.inventory.application.queries
   - com.example.inventory.application.handlers
   - com.example.inventory.application.dto
2. Implement immutable command/value objects:
   - PlaceOrderCommand
   - CancelOrderCommand
   - ProcessPaymentCommand
   - ShipOrderCommand
   - DeliverOrderCommand
   - GetOrderQuery
   - GetInventoryQuery
   - ListOrdersQuery
3. Implement request/response DTOs:
   - CreateOrderRequest
   - CreateOrderItemRequest
   - OrderResponse
   - InventoryItemResponse
4. Ensure the DTOs are simple POJOs or records and contain no persistence or web annotations.

### Deliverables
- Package structure for the application layer
- Immutable command/query objects
- DTOs for request and response mapping

---

## Phase 2 — Implement the command handlers

### Goal
Add the orchestration layer for order lifecycle use cases.

### Tasks
1. Implement PlaceOrderCommandHandler with the following responsibilities:
   - receive a PlaceOrderCommand
   - resolve the product catalog from a repository or in-memory adapter
   - validate the request shape and stock availability
   - create an Order through the domain aggregate
   - persist the order through the domain repository
   - return an OrderResponse
2. Implement ProcessPaymentCommandHandler:
   - load an existing order
   - call the aggregate payment transition
   - persist the updated order state
   - return updated order information
3. Implement CancelOrderCommandHandler:
   - load the order by id
   - invoke the aggregate cancellation rule
   - persist the updated order state
4. Implement ShipOrderCommandHandler and DeliverOrderCommandHandler:
   - load the order
   - invoke the domain lifecycle method
   - persist the updated state
5. Define and use application-facing exceptions for invalid input or invalid state.

### Deliverables
- Five command handlers covering the core order lifecycle
- Use-case orchestration without domain business logic leakage
- Basic handler-level validation and error handling

---

## Phase 3 — Implement the query handlers

### Goal
Provide read-model projections for order and inventory queries.

### Tasks
1. Implement GetOrderQueryHandler:
   - load the order from the repository
   - map the domain object to an OrderResponse read model
   - avoid returning the domain aggregate directly
2. Implement ListOrdersQueryHandler:
   - load orders for a customer or by status
   - project them into response DTOs
3. Implement GetInventoryQueryHandler:
   - load the current inventory projection from the product catalog repository
   - map each product to InventoryItemResponse
4. Keep the query handlers completely independent from the domain entity implementation details.

### Deliverables
- Query handlers that return read models
- Clear separation between write-side orchestration and read-side projection

---

## Phase 4 — Add unit tests for handlers and DTO mapping

### Goal
Verify that the application layer behaves correctly without coupling to infrastructure.

### Tasks
1. Add unit tests for each handler using simple fake repositories or in-memory doubles.
2. Cover the acceptance criteria:
   - valid place-order command creates a pending order
   - payment command moves order to PAID
   - cancel command moves order to CANCELLED
   - query handlers return read models
   - invalid input raises an application or domain exception
3. Avoid testing infrastructure concerns; focus on orchestration and mapping behavior.

### Deliverables
- Unit tests for the application layer
- Coverage for main success and failure flows

---

## Phase 5 — Integrate and verify the application layer

### Goal
Ensure the application layer is coherent, reusable, and testable.

### Tasks
1. Review the handlers for statelessness and reusability.
2. Verify that the domain layer owns all business rules.
3. Run the Maven test suite and ensure the application-layer tests pass.
4. Confirm that the commands and queries are simple immutable objects with no persistence or web dependency.

### Deliverables
- Passing application-layer implementation
- Clean separation between application, domain, and infrastructure concerns
- Confidence that the layer is ready for the next spec

---

## Definition of Done for Spec 2
- The application layer contains command/query objects and handlers for the specified use cases.
- Handlers orchestrate domain behavior and return DTO/read-model responses.
- The application layer has no direct Spring persistence or web framework dependencies.
- Unit tests cover the happy path and invalid input paths.
- Maven tests pass successfully.
