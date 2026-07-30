# Implementation Plan for Spec 3

This plan breaks Spec 3 into concrete implementation phases while staying aligned with the project constitution and the domain model established in the earlier specs.

## Constitutional Guardrails
- The domain layer must remain free of persistence imports and JPA annotations.
- The infrastructure layer may depend on Spring, JPA, and PostgreSQL.
- Repository implementations must preserve aggregate boundaries and expose only domain objects to the application layer.
- The persistence design must support optimistic locking and correct mapping of child entities.
- The implementation must be verifiable with Maven tests and integration-style persistence tests.

---

## Phase 1 — Create the persistence package structure and configuration

### Goal
Establish the infrastructure foundation for JPA persistence and repository wiring.

### Tasks
1. Create the following packages under the infrastructure layer:
   - com.example.inventory.infrastructure.persistence.jpa
   - com.example.inventory.infrastructure.persistence.config
2. Add the persistence configuration class, PersistenceConfig, to define:
   - Spring Data JPA scanning
   - transaction management setup
   - any required bean configuration for repository support
3. Update the application configuration to support PostgreSQL persistence for local development.
4. Ensure the persistence layer is wired so the application can use the repositories without changing the domain layer.

### Deliverables
- Infrastructure package structure
- Persistence configuration class
- Base application configuration for database access

---

## Phase 2 — Implement the JPA entity model

### Goal
Represent the domain aggregates and value objects in a persistence-friendly form while preserving the domain contract.

### Tasks
1. Implement OrderJpaEntity with the fields required by the spec:
   - id
   - customerId
   - status
   - totalAmount
   - currency
   - createdAt
   - updatedAt
   - reservedUntil
   - version
2. Implement OrderItemJpaEntity with the necessary order-item persistence fields:
   - orderId
   - productId
   - quantity
   - unitPrice
   - sku
3. Implement ProductJpaEntity with inventory-related persistence fields:
   - name
   - description
   - price
   - currency
   - quantityInStock
   - version
4. Implement CustomerJpaEntity with customer address fields.
5. Add optimistic locking support using @Version on the versioned entities.
6. Add the appropriate JPA relationships for orders and their items, keeping the mapping explicit and easy to reason about.

### Deliverables
- Four JPA entity classes with versioning and persistence fields
- PostgreSQL-compatible entity definitions
- Clear ownership of child entities within the aggregate

---

## Phase 3 — Create the domain-to-entity mapping layer

### Goal
Isolate persistence concerns from the domain model via mapper classes.

### Tasks
1. Implement OrderJpaMapper to translate between:
   - Order domain aggregate and OrderJpaEntity
   - OrderJpaEntity and Order domain aggregate
2. Implement ProductJpaMapper to translate between:
   - Product domain entity and ProductJpaEntity
   - ProductJpaEntity and Product domain entity
3. Ensure the mapping layer handles:
   - simple scalar fields
   - nested order items
   - monetary values and currencies
   - status values and timestamps
4. Keep the mappers focused on translation only; avoid embedding business rules inside them.

### Deliverables
- OrderJpaMapper implementation
- ProductJpaMapper implementation
- Clean translation between domain and persistence models

---

## Phase 4 — Implement repository adapters for orders and products

### Goal
Provide concrete infrastructure implementations for the domain repository contracts.

### Tasks
1. Implement JpaOrderRepository to satisfy the domain OrderRepository contract.
2. Implement JpaProductRepository to satisfy the domain ProductRepository contract.
3. In the repository implementations:
   - create or update JPA entities through the mappers
   - save and load domain aggregates and entities
   - preserve aggregate boundaries when reading and writing orders and their items
4. Ensure repository methods accept and return domain objects, not persistence entities.
5. Handle persistence exceptions in a controlled way so the application layer receives domain-relevant failures.

### Deliverables
- Working order repository implementation backed by JPA
- Working product repository implementation backed by JPA
- Repository abstractions that remain invisible to the domain layer

---

## Phase 5 — Add persistence tests and concurrency coverage

### Goal
Verify that the infrastructure layer behaves correctly for normal persistence, reload, and concurrency scenarios.

### Tasks
1. Add repository integration tests for:
   - saving an order aggregate and reloading it successfully
   - loading a product entity and reconstructing the domain object
   - loading an order with items and ensuring child rows are present
2. Add a concurrency test for optimistic locking:
   - update the same versioned entity twice
   - confirm the second update throws an OptimisticLockException or equivalent persistence exception
3. Use the test profile or an in-memory database for fast unit/integration verification where appropriate.
4. Keep tests focused on persistence behavior rather than duplicating domain logic.

### Deliverables
- Persistence integration tests for orders and products
- Optimistic locking test coverage
- Confidence that the repository layer satisfies the acceptance criteria

---

## Phase 6 — Verify architecture and finalize the persistence layer

### Goal
Ensure the persistence implementation is complete, consistent, and ready for the next specification.

### Tasks
1. Review the infrastructure layer for any accidental coupling from the domain layer.
2. Confirm that the domain layer still has no persistence imports or annotations.
3. Verify that JPA entities are mapped correctly to the database schema and that the configuration is suitable for local development and testing.
4. Run the Maven test suite and confirm the persistence tests pass.
5. Review the code for naming consistency and package structure alignment with the project conventions.

### Deliverables
- Fully integrated persistence layer
- Passing Maven test suite with persistence coverage
- Clean separation between application, domain, and infrastructure responsibilities

---

## Definition of Done for Spec 3
- The infrastructure package contains the required JPA entities, mappers, repositories, and persistence configuration.
- Orders and products can be persisted and reloaded through the repository layer.
- Optimistic locking is implemented and verified.
- The domain layer remains free of persistence concerns.
- Maven tests pass, including persistence-focused tests.
