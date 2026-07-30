# Implementation Plan for Spec 4

This plan breaks Spec 4 into concrete implementation phases while staying aligned with the project constitution and the domain model established in earlier specs.

## Constitutional Guardrails
- The domain layer must remain free of persistence and scheduling concerns.
- The infrastructure layer may depend on Spring, JPA, and Envers.
- Scheduled jobs must delegate to the application layer rather than performing domain transitions directly.
- The reservation-expiry rule must remain in the domain aggregate and be reused by the job.
- The implementation must be verifiable with Maven tests and infrastructure-focused integration tests.

---

## Phase 1 — Wire up Envers-based audit infrastructure

### Goal
Enable revision tracking for order changes and capture audit metadata.

### Tasks
1. Add the Envers dependency and required Spring configuration if it is not already present in the project.
2. Create the infrastructure audit package and add the following classes:
   - EnversConfig
   - AuditRevisionEntity
   - AuditRevisionListener
3. Configure Envers so it can create revision tables and register the audit listener.
4. Enable auditing on the Order domain entity and ensure its updates produce revision entries.
5. Capture revision metadata such as the revision timestamp and the user identity in a way that is transparent to the domain layer.

### Deliverables
- Envers configuration class
- Revision entity and listener
- Audited order persistence flow with revision creation

---

## Phase 2 — Implement the reservation timeout job

### Goal
Provide a periodic job that finds expired pending orders and cancels them through the application layer.

### Tasks
1. Create ReservationTimeoutJob in the infrastructure jobs package.
2. Configure the job to run on a fixed schedule such as every minute.
3. Inject the appropriate application service or handler that can cancel orders.
4. Query for pending orders whose reservation window has expired.
5. For each matching order, invoke the application-layer cancellation flow rather than implementing state transition logic inside the job.
6. Ensure the job uses the domain method isReservationExpired() or an equivalent aggregate-level check rather than duplicating the business rule.

### Deliverables
- ReservationTimeoutJob implementation
- Scheduled execution wiring
- Job behavior that delegates to the application layer

---

## Phase 3 — Add audit and job tests

### Goal
Verify that the audit feature and scheduled job behave correctly for normal and edge cases.

### Tasks
1. Add an integration test for order updates that confirms an audit revision is written.
2. Add a test for an expired pending order to ensure the job cancels it.
3. Add a test for a non-expired pending order to ensure the job leaves it pending.
4. Use the test profile or an in-memory database where appropriate to keep the tests fast and deterministic.
5. Keep the tests focused on persistence and scheduling behavior rather than duplicating domain logic.

### Deliverables
- Audit revision tests
- Reservation timeout job tests
- Confidence that both acceptance criteria are covered

---

## Phase 4 — Verify architecture and finalize the infrastructure layer

### Goal
Ensure the implementation is consistent, stable, and ready for the next specification.

### Tasks
1. Review the infrastructure code to confirm the domain layer remains free of persistence and scheduler concerns.
2. Verify that the scheduled job only orchestrates work and does not contain direct state-transition logic.
3. Run the Maven test suite and confirm the new audit and job tests pass.
4. Review package naming, class responsibilities, and configuration clarity for maintainability.

### Deliverables
- Completed audit and scheduled-job infrastructure layer
- Passing Maven test suite with new coverage
- Clean separation between application, domain, and infrastructure responsibilities

---

## Definition of Done for Spec 4
- The infrastructure package contains the required audit and scheduled-job classes.
- Order updates create audit revisions through Envers.
- The reservation timeout job cancels expired pending orders and leaves non-expired orders unchanged.
- The domain rule for reservation expiry remains in the domain aggregate.
- Maven tests pass, including the new audit and scheduled-job coverage.
