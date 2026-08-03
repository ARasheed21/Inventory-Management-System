# Hexagonal Architecture

## Status
Accepted

## Context
The project needs a maintainable and testable business architecture where the domain model remains independent from transport, persistence, and security infrastructure.

## Decision
Use a hexagonal / clean architecture split with four main layers:
- domain: business entities, domain rules, and repository contracts
- application: command/query handlers and orchestration
- infrastructure: JPA adapters, scheduled job execution, Envers, and security configuration
- web: REST controllers, DTOs, and OpenAPI annotations

## Consequences
Benefits:
- clear separation of concerns
- easy substitution of infrastructure implementations
- better testability of business rules

Trade-offs:
- additional mapping and delegation code between layers
- more explicit package boundaries during development
