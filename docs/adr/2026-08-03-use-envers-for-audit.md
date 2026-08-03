# Use Hibernate Envers for Auditability

## Status
Accepted

## Context
The project needs a reliable change trail for order state and lifecycle records without rewriting the domain model around audit concerns.

## Decision
Use Hibernate Envers to automatically generate revision tables for the order entity and related audited data.

## Consequences
Benefits:
- transparent historical revision tracking
- simplified persistence auditability
- alignment with the repository’s audit requirements

Trade-offs:
- additional schema artifacts for revision tables
- additional integration complexity during persistence verification
