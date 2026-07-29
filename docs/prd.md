# PRD: Enterprise Modular Monolith - Inventory & Order Management System

## Problem Statement

Backend developers learning Spring Boot often struggle to understand how to properly structure large-scale enterprise applications. Many tutorials focus on simple CRUD operations or microservices, leaving a gap in understanding how to build maintainable, production-grade monoliths with complex business logic. The learner needs a realistic project that demonstrates proper architectural patterns, domain-driven design, and enterprise-grade implementations without the overhead of distributed systems complexity.

## Solution

A modular monolith application implementing an Inventory & Order Management System using Hexagonal/Clean Architecture. The system will manage product inventory, order processing with a 15-minute timed reservation system, and order state transitions. The project serves as a comprehensive learning vehicle for mastering Spring Boot, JPA, Security, and testing while producing a portfolio-worthy artifact.

## User Stories

1. As a system administrator, I want to add new products to the inventory, so that they can be sold to customers

2. As a system administrator, I want to update product information (name, price, description), so that inventory data remains accurate

3. As a system administrator, I want to view current inventory levels, so that I can monitor stock availability

4. As a system administrator, I want to adjust inventory quantities manually, so that I can correct discrepancies

5. As a customer, I want to browse available products, so that I can decide what to purchase

6. As a customer, I want to view product details including price and availability, so that I can make informed purchasing decisions

7. As a customer, I want to add items to a shopping cart, so that I can prepare an order

8. As a customer, I want to place an order for items in my cart, so that I can purchase products

9. As a customer, I want my ordered items to be reserved for 15 minutes after placing the order, so that I have time to complete payment

10. As a customer, I want to see a countdown timer showing how long my reservation will last, so that I know how much time I have to pay

11. As a customer, I want to complete payment for my order, so that the reservation becomes a confirmed order

12. As a customer, I want to receive a notification if my payment fails, so that I understand why my order wasn't processed

13. As a customer, I want my order to be automatically cancelled if I don't complete payment within 15 minutes, so that inventory is released back to the system

14. As a customer, I want to view the status of my orders (PENDING, PAID, SHIPPED, DELIVERED, CANCELLED), so that I know where my order is in the fulfillment process

15. As a customer, I want to cancel a pending order before payment, so that I can change my mind without waiting for timeout

16. As a customer, I want to view my order history, so that I can track past purchases

17. As a warehouse worker, I want to see a list of paid orders ready for shipping, so that I can fulfill them

18. As a warehouse worker, I want to mark orders as shipped, so that the system updates order status

19. As a warehouse worker, I want to mark orders as delivered, so that the fulfillment process is complete

20. As a system administrator, I want to view the complete audit history of all order changes, so that I can investigate any issues

21. As a system administrator, I want to view who made changes to orders and when, so that I can maintain accountability

22. As a customer, I want my order total to include proper currency formatting and calculation, so that I know exactly what I'm paying

23. As a system administrator, I want to access the system securely with role-based access, so that only authorized personnel can perform administrative actions

24. As a customer, I want to register and authenticate with the system, so that my orders are associated with my account

25. As a system developer, I want all domain logic to be isolated from infrastructure concerns, so that the business rules are maintainable and testable

26. As a system developer, I want to use Command Query Separation, so that commands and queries have clear, separate responsibilities

27. As a system developer, I want automatic audit logging for all order entities, so that I don't need to manually implement audit trails

28. As a system administrator, I want to prevent inventory overselling through optimistic locking, so that two customers can't purchase the same item simultaneously

29. As a system administrator, I want to be able to view current reserved inventory, so that I understand what stock is currently allocated to pending orders

30. As a system developer, I want comprehensive test coverage of domain logic, so that business rules are thoroughly verified

31. As a system administrator, I want to view orders by their current state, so that I can monitor the pipeline of order processing

32. As a customer, I want to receive confirmation of my order placement with an order number, so that I can reference it later

33. As a system administrator, I want inventory adjustments to be logged and tracked, so that changes to stock are auditable

34. As a system developer, I want the application to be containerized and run with real infrastructure for testing, so that integration tests are realistic

## Implementation Decisions

### Architecture Pattern
- **Hexagonal/Clean Architecture** with clear separation between domain, application, infrastructure, and web layers
- Domain layer contains all business logic and is dependency-free
- Application layer orchestrates use cases and transactions
- Infrastructure layer handles external concerns (JPA, security, audit)
- Web layer handles REST API endpoints and DTO mapping

### CQRS Implementation
- **Commands** for state mutation: PlaceOrderCommand, CancelOrderCommand, ProcessPaymentCommand, ShipOrderCommand, DeliverOrderCommand
- **Queries** for data retrieval with specific Projections/Read Models: OrderSummaryProjection, InventoryProjection
- Commands follow the Command pattern with validation encapsulated in the command handler

### Domain-Driven Design Patterns
- **Aggregate Root**: Order entity encapsulating all order-related business logic
- **Value Objects**: Money (with currency), Address, SKU (Stock Keeping Unit), OrderStatus
- **Domain Events**: OrderPlacedEvent, PaymentProcessedEvent, OrderCancelledEvent, OrderShippedEvent
- **Repository Pattern**: Domain interfaces for OrderRepository and ProductRepository

### State Machine
- Order states: PENDING → PAID → SHIPPED → DELIVERED | CANCELLED
- State transitions with validation:
  - PENDING → PAID (payment successful)
  - PENDING → CANCELLED (user cancels or timeout expires)
  - PAID → SHIPPED (warehouse fulfills)
  - SHIPPED → DELIVERED (delivery confirmed)
- State machine implemented as part of the Order aggregate root

### Reservation System
- 15-minute timed reservation when order is placed in PENDING state
- Reservation timeout managed by scheduled job checking for expired reservations
- On timeout: order status changes to CANCELLED, inventory quantities restored
- Reservation tracked via Order.createdAt timestamp and scheduled expiration check

### Optimistic Locking
- `@Version` annotation on Order entity to prevent concurrent modifications
- JPA automatically handles version checking on updates
- Prevents overselling inventory when multiple orders try to reserve the same item

### Audit Trail
- **Hibernate Envers** configured for Order entity auto-auditing
- Automatic tracking of all changes: who, when, what changed, old/new values
- REVINFO table for revision metadata (user, timestamp, IP)

### Security Implementation
- **OAuth2 Resource Server** using Keycloak as identity provider
- JWT token validation for all endpoints
- **Method-level security** with `@PreAuthorize("hasRole('ADMIN')")` for administrative endpoints
- Roles: ADMIN, WAREHOUSE, CUSTOMER

### Module Structure
The application is organized into the following modules/packages:
- **domain**: Aggregate roots, value objects, domain services, repository interfaces, events
- **application**: Use case implementations, command/query handlers, DTOs, interfaces
- **infrastructure**: JPA entities, Spring Data repositories, Hibernate Envers configuration, security configuration, scheduled jobs
- **web**: REST controllers, DTO mapping, exception handling, API documentation

### API Contract Patterns
- REST API with JSON payloads
- POST `/api/orders` - Place order
- GET `/api/orders/{orderId}` - Get order details
- GET `/api/orders` - List orders (filtered by status, customer)
- POST `/api/orders/{orderId}/payment` - Process payment
- POST `/api/orders/{orderId}/cancel` - Cancel order
- PUT `/api/orders/{orderId}/status` - Update order status (admin)
- GET `/api/inventory` - View inventory levels
- POST `/api/inventory/products` - Add new product (admin)
- PUT `/api/inventory/products/{productId}` - Update product (admin)

### Prototype Snippet: Order State Machine
The following state machine logic encodes the business rules for order lifecycle:

```java
public enum OrderStatus {
    PENDING, PAID, SHIPPED, DELIVERED, CANCELLED;
    
    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == PAID || target == CANCELLED;
            case PAID -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == DELIVERED || target == CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
```

### Database Schema
- **orders**: id, customer_id, total_amount, currency, status, version, created_at, updated_at, reserved_until
- **order_items**: id, order_id, product_id, quantity, unit_price, sku
- **products**: id, name, description, price, currency, quantity_in_stock, version
- **customers**: id, name, email, address (embedded)
- Audit tables generated automatically by Envers

### Scheduled Jobs
- **ReservationTimeoutJob**: Runs every minute to cancel pending orders older than 15 minutes

## Testing Decisions

### Testing Strategy
- **Domain Layer (95%+ coverage target)**: Pure unit tests with no external dependencies
  - Test domain logic in isolation using mock/spy for dependencies
  - Test state machine transitions thoroughly
  - Test reservation logic and expiration

### Test Categories
- **Unit Tests**: Domain entities, value objects, domain services
- **Integration Tests**: Repository operations, JPA mappings, Envers auditing
- **Controller Tests**: REST endpoint validation, security, DTO mapping
- **E2E Tests**: Full flow tests using Testcontainers

### Test Seams
1. **Domain Layer**: Repository interfaces mocked with Mockito; domain logic tested directly
2. **Application Layer**: Use cases tested with mocked repositories and domain services
3. **Infrastructure Layer**: Testcontainers for real PostgreSQL and Keycloak integration
4. **Web Layer**: `@WebMvcTest` with mocked service layer; `@SpringBootTest` with Testcontainers for full integration

### Prior Art in Codebase
- Domain tests use JUnit 5 and AssertJ for fluent assertions
- Integration tests use `@DataJpaTest` with Testcontainers
- Controller tests use `@WebMvcTest` with `@MockBean` for services
- Full stack tests use `@SpringBootTest` with `@Testcontainers`

### Testcontainers Configuration
- PostgreSQL container for repository tests
- Keycloak container for security tests (optional, can use mocked JWT)

## Out of Scope

- Distributed transactions or microservices patterns
- Message queues or event-driven architecture
- Kubernetes deployment or container orchestration
- Complex CQRS with separate read/write stores
- Event sourcing (uses Envers for audit instead)
- Full-fledged frontend/UI implementation (REST API only)
- Payment gateway integration (mock payment processing)
- Complex pricing rules or promotions/discounts
- Internationalization or localization
- High-availability or clustering configurations
- Complex reporting or data warehousing
- Spring Cloud or service discovery

## Further Notes

### Learning Objectives Covered
This project maps directly to Phases 0-4 of the Spring Boot learning curriculum:

- **Phase 0 (Core Java)**: Domain models, value objects, enums, validation
- **Phase 1 (Design Patterns)**: CQRS, DDD patterns, repository pattern, builder pattern
- **Phase 2 (Spring Core)**: Dependency injection, Spring Boot configuration, component scanning
- **Phase 3 (Data/JPA)**: Hibernate mappings, Envers, optimistic locking, transaction management
- **Phase 4 (Security)**: OAuth2 Resource Server, method-level security, role-based access

### Portfolio Value
The completed project demonstrates:
- Clean/Hexagonal architecture implementation
- Complex business logic with state machines and reservation systems
- Real-world enterprise patterns (DDD, CQRS, auditing)
- Production-ready security setup with OAuth2
- Comprehensive testing strategy with 95%+ domain coverage
- Infrastructure integration with Testcontainers

### CV Bullet Points
"Designed a modular monolith using Hexagonal Architecture, implementing a 15-min timed inventory reservation system with optimistic locking to prevent race conditions, achieving 100% audit trail via Hibernate Envers."

### Project Timeline Estimate
- Phase 0/1 (Domain & Core Setup): 2-3 days
- Phase 2 (Spring Configuration): 1-2 days  
- Phase 3 (JPA & Envers): 3-4 days
- Phase 4 (Security): 2-3 days
- Testing & Integration: 2-3 days
- **Total: ~10-15 days**

---

*This PRD was synthesized from the project description and learning objectives for the "Enterprise Modular Monolith" project. The implementation decisions align with the specified learning phases and technical requirements.*