# Spec 4: Infrastructure – Audit (Envers) & Scheduled Jobs

## 1. Spec Metadata
- **Name**: Infrastructure – Audit (Envers) & Scheduled Jobs
- **Dependencies**: Spec 1, Spec 3
- **Estimated Effort**: Medium

## 2. In-Scope Artifacts
After this spec is complete, the following artifacts should exist:

- Package: `com.example.inventory.infrastructure.audit`
  - `EnversConfig`
  - `AuditRevisionEntity`
  - `AuditRevisionListener`

- Package: `com.example.inventory.infrastructure.jobs`
  - `ReservationTimeoutJob`

## 3. Core Domain Models & Contracts

### Envers Configuration
- Enable auditing on the `Order` entity.
- Ensure revision data captures the user identity and timestamp.

### Reservation Timeout Job
- Runs periodically (for example every minute).
- Finds pending orders whose reservation is expired.
- Calls the application-layer cancellation logic.

## 4. Behavioral Specifications
1. When an order is updated, Envers creates a new revision entry.
2. When a pending order exceeds its reservation window, the scheduled job cancels it.
3. The job should use the domain method `isReservationExpired()` rather than duplicating business logic.

## 5. Input / Output Contracts
- No web contracts required.
- The job uses repository queries and application handlers internally.

## 6. Technical Constraints / Non-Functional Rules
- The scheduled job must not contain domain state transition logic directly.
- The domain rule for timeout must live in the aggregate.
- Envers must be configured in a way that is transparent to the domain layer.

## 7. Acceptance Criteria & Test Matrix
1. Given an order update, when the entity is persisted, then an audit revision is written.
2. Given an expired pending order, when the job runs, then the order is cancelled.
3. Given a non-expired pending order, when the job runs, then it remains pending.
