# Implementation Plan for Spec 7

This plan breaks Spec 7 into concrete implementation phases and keeps the work aligned with the project constitution and the repository’s existing DDD/Spring architecture.

## Constitutional Guardrails
- Test coverage must remain above the repository constitution threshold.
- Tests must be deterministic, isolated, and repeatable.
- Performance benchmarks must be run in a controlled environment with a reproducible dataset.
- The test suite must remain green under Maven verification.
- The benchmark must exercise the real timeout/scheduled-job path rather than only mock behavior.

---

## Phase 1 — Establish the test harness foundation

### Goal
Prepare the project for reliable automated verification across unit, integration, web, and benchmark scenarios.

### Tasks
1. Confirm the test dependency stack in Maven:
   - JUnit 5
   - Spring Boot test support
   - Mockito or equivalent for focused unit isolation
   - Testcontainers for PostgreSQL-backed integration tests
2. Add or align the test configuration profiles:
   - `application-test.yml`
   - PostgreSQL test container wiring
   - deterministic test seed data strategy
3. Create shared test fixtures and builders for:
   - products
   - orders
   - order items
   - customer identifiers
4. Add a reusable benchmark dataset generator for pending orders.
5. Establish a consistent naming convention for test classes and test data packages.

### Deliverables
- Stable test configuration for repository and web tests
- Reusable domain/application fixture builders
- Controlled benchmark data generation strategy
- Baseline test harness ready for phased implementation

---

## Phase 2 — Add domain and application unit coverage

### Goal
Prove that domain state transitions and application handler behavior are correct before verifying the fuller system flow.

### Tasks
1. Add unit tests for the domain aggregate root transitions, including:
   - pending → paid
   - pending → cancelled
   - paid → shipped
   - shipped → delivered
   - invalid transition rejection
   - reservation expiry logic
2. Add unit tests for value objects and domain validation rules.
3. Add handler-level tests for the application layer using the real command/query handlers and controlled in-memory or fake repository behavior.
4. Verify that each test asserts the actual state result, not a mocked intermediate artifact.
5. Add negative-path tests for:
   - insufficient stock
   - invalid quantity or item data
   - duplicate or inconsistent business conditions

### Deliverables
- Domain unit test suite covering state lifecycle rules
- Application handler unit tests covering business orchestration
- Clear regression protection for business logic correctness

---

## Phase 3 — Implement repository and infrastructure integration tests

### Goal
Verify persistence correctness with realistic database-backed integration coverage.

### Tasks
1. Add repository integration tests using Testcontainers with PostgreSQL.
2. Verify persistence behavior for:
   - create order
   - read order by id
   - list orders with filters
   - order status updates
   - auditable persistence behavior
3. Add timeout/job integration coverage for the scheduled cancellation workflow.
4. Confirm that `@Transactional`, entity mapping, and revision/audit behavior are exercised through the real persistence stack.
5. Add assertions for data integrity in both the happy path and failure path.

### Deliverables
- Repository integration coverage for persistence correctness
- Scheduled timeout workflow verification
- Real database-backed confidence in infrastructure behavior

---

## Phase 4 — Add web and security integration tests

### Goal
Validate the external HTTP boundary, authorization behavior, and request validation as exposed by the controller layer.

### Tasks
1. Add web integration tests using MockMvc and the real application security context.
2. Cover the controller contract for:
   - order creation
   - order lookup
   - order listing
   - payment processing
   - cancellation
3. Validate that the security rules block or allow requests as expected for the defined role model.
4. Add validation tests for malformed input, missing fields, and business-rule rejection responses.
5. Assert response shape, status codes, and error payloads through the real HTTP layer.

### Deliverables
- Controller-level integration test coverage
- Security-aware API behavior verification
- Validation and response-shape contract protection

---

## Phase 5 — Add the end-to-end lifecycle test

### Goal
Run a single flow that exercises the main business journey across the real stack boundaries.

### Tasks
1. Build an E2E-style test that runs the workflow:
   - place order
   - pay
   - ship
   - deliver
2. Use realistic fixtures and realistic repository/config state.
3. Verify that each step records the expected domain transition, persisted state, and externally visible response.
4. Keep the E2E test deterministic by controlling runtime dependencies and seed data.
5. Ensure the E2E flow is part of the automated verification path, not a manual-only test.

### Deliverables
- End-to-end lifecycle test covering the primary order workflow
- Proven real-system transition path from API to persistence and domain state
- Regression guard for the business lifecycle contract

---

## Phase 6 — Implement the performance benchmark and constrained run

### Goal
Prove that the timeout/cancellation workflow scales to the required benchmark size within the defined time budget.

### Tasks
1. Implement a benchmark test class dedicated to the pending-order cancellation scenario.
2. Programmatically create 1,000 pending orders with the benchmark fixture strategy.
3. Run the timeout job or its business entry point through a controlled test harness.
4. Capture the execution time and assert that all 1,000 orders are cancelled within 5 seconds.
5. Isolate the benchmark environment to reduce noise from unrelated system activity.
6. Keep benchmark execution in a controlled, dedicated profile or test class so it remains repeatable.

### Deliverables
- Performance benchmark test for timeout cancellation
- Deterministic benchmark dataset generation
- Proof that the scheduled cancellation path satisfies the benchmark target

---

## Phase 7 — Run verification and quality gates

### Goal
Turn the new test assets into a repository-wide proof artifact.

### Tasks
1. Run the focused test suites for the new test classes.
2. Run the full Maven regression suite.
3. Confirm JaCoCo coverage remains above the constitution threshold.
4. Review any flaky or timing-sensitive assertions and stabilize them.
5. Verify that benchmark results are reproducible in the current environment.
6. Record any known test environment limits, such as container startup time or controlled timing assumptions.

### Deliverables
- Passing repository test suite
- Verified benchmark result evidence
- Coverage and quality gate confirmation
- Final test evidence suitable for the completion record

---

## Definition of Done for Spec 7
- Unit tests cover the aggregate lifecycle and domain validation behavior.
- Repository integration tests use the real persistence path and verify persistence correctness.
- Web/security integration tests verify controller behavior and status/error contracts.
- An E2E-style lifecycle test proves the primary business flow from order placement to completion.
- A performance benchmark test proves that 1,000 pending orders are cancelled within 5 seconds.
- Maven regression verification passes with clean test evidence and acceptable coverage.
