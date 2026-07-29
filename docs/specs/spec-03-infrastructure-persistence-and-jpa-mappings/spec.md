# Spec 3: Infrastructure – Persistence & JPA Mappings

## 1. Spec Metadata
- **Name**: Infrastructure – Persistence & JPA Mappings
- **Dependencies**: Spec 1, Spec 2
- **Estimated Effort**: High

## 2. In-Scope Artifacts
After this spec is complete, the following artifacts should exist:

- Package: `com.example.inventory.infrastructure.persistence.jpa`
  - `OrderJpaEntity`
  - `OrderItemJpaEntity`
  - `ProductJpaEntity`
  - `CustomerJpaEntity`
  - `JpaOrderRepository`
  - `JpaProductRepository`
  - `OrderJpaMapper`
  - `ProductJpaMapper`

- Package: `com.example.inventory.infrastructure.persistence.config`
  - `PersistenceConfig`

## 3. Core Domain Models & Contracts

### JPA Entities
- `OrderJpaEntity`
  - fields: `Long id`, `String customerId`, `String status`, `BigDecimal totalAmount`, `String currency`, `Instant createdAt`, `Instant updatedAt`, `Instant reservedUntil`, `Long version`
- `OrderItemJpaEntity`
  - fields: `Long id`, `Long orderId`, `String productId`, `Integer quantity`, `BigDecimal unitPrice`, `String sku`
- `ProductJpaEntity`
  - fields: `Long id`, `String name`, `String description`, `BigDecimal price`, `String currency`, `Integer quantityInStock`, `Long version`
- `CustomerJpaEntity`
  - fields: `Long id`, `String name`, `String email`, `String street`, `String city`, `String state`, `String postalCode`, `String country`

### Repository Implementations
- `JpaOrderRepository` should implement the domain `OrderRepository` interface.
- `JpaProductRepository` should implement the domain `ProductRepository` interface.

## 4. Behavioral Specifications
1. Domain objects are mapped to JPA entities through mapper classes.
2. The persistence layer persists orders and products to PostgreSQL.
3. Optimistic locking is enabled via `@Version` on the entity classes.
4. The repository implementations translate between the domain model and the database structure while preserving the aggregate boundaries.

## 5. Input / Output Contracts
- Repository methods should accept and return domain objects.
- The infrastructure layer is responsible for converting between domain and persistence models.

## 6. Technical Constraints / Non-Functional Rules
- The infrastructure layer may depend on Spring and JPA.
- The domain layer must remain free from persistence imports.
- Use PostgreSQL-compatible schema definitions.
- Configure `spring.jpa.hibernate.ddl-auto` appropriately for local development and testing.

## 7. Acceptance Criteria & Test Matrix
1. Given an order aggregate, when it is saved through the repository, then it is persisted successfully.
2. Given two concurrent updates to the same versioned entity, when the second update occurs, then a locking exception is thrown.
3. Given a product entity, when it is loaded, then the domain object is reconstructed correctly.
4. Given an order with items, when retrieved from the database, then the items are also loaded correctly.
