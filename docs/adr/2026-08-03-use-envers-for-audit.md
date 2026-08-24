# Use Hibernate Envers for Auditability

## Status
Accepted

## Context
The project needs a reliable change trail for order state and lifecycle records without rewriting the domain model around audit concerns.

## Decision
Use Hibernate Envers to automatically generate revision tables for audited entities. Both the
order aggregate (`OrderJpaEntity`, `OrderItemJpaEntity`) and the product/stock entity
(`ProductJpaEntity`) are `@Audited`; revision metadata captures the acting user
(`AuditRevisionListener`). History is exposed read-only through admin endpoints
(`/api/admin/audit/products/{id}`, `/api/admin/audit/orders/{id}`).

## Consequences
Benefits:
- transparent historical revision tracking
- simplified persistence auditability
- alignment with the repository’s audit requirements

Trade-offs:
- additional schema artifacts for revision tables
- additional integration complexity during persistence verification
