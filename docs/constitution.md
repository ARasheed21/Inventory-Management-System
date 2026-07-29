# Project Constitution  
**Enterprise Modular Monolith – Inventory & Order Management**

---

## 1. Purpose

This Constitution establishes the non‑negotiable rules, standards, and governance model for the project. All development work must comply with these principles. The Constitution is the single source of truth for **how** we build the system; the PRD defines **what** we build.

---

## 2. Guiding Architectural Principles

These principles derive directly from the PRD and must be upheld in every code change.

### 2.1 Hexagonal / Clean Architecture
- **Domain layer** (entities, value objects, aggregates, domain services, repository interfaces) must have **zero dependencies** on frameworks (Spring, JPA, etc.).
- **Application layer** (use cases, commands, queries, handlers) orchestrates the domain but contains **no business logic**; it only delegates to domain services/aggregates.
- **Infrastructure layer** (JPA implementations, Envers, Spring Security, scheduled jobs) depends on the application and domain layers – never the reverse.
- **Web layer** (REST controllers, DTO mappers) is the entry point; it converts HTTP requests to commands/queries and returns DTOs.

### 2.2 Domain‑Driven Design (DDD)
- **Aggregate Root** = `Order`. All modifications to an order go through the aggregate root.
- **Value Objects** = `Money`, `Address`, `SKU`, `OrderStatus`. They are immutable and self‑validating.
- **Repositories** are defined as interfaces in the domain and implemented in infrastructure.
- **Domain events** (e.g., `OrderPlacedEvent`) must be used to trigger side effects (e.g., reservation timeout scheduling).

### 2.3 Command Query Separation (CQRS)
- **Commands** (`PlaceOrderCommand`, `CancelOrderCommand`, etc.) mutate state.
- **Queries** retrieve data via **Projections/Read Models** – never return domain entities directly to the web layer.
- Commands and queries are handled by dedicated handlers in the application layer.

### 2.4 State Machine Enforcement
- Order state transitions must follow the defined graph:
  `PENDING → PAID → SHIPPED → DELIVERED | CANCELLED`
- Transition logic resides **inside** the `Order` aggregate root – no external service may change status without invoking the aggregate’s method.

---

## 3. Technology Stack & Coding Standards

### 3.1 Build & Environment
- **Build Tool**: Apache Maven (pom.xml).
- **Java Version**: The AI agent must detect the installed JDK version and configure the `maven-compiler-plugin` accordingly (source/target = detected version). No hard‑coded version in the constitution.
- **Packaging**: Executable JAR.

### 3.2 Library Selection
- No mandatory third‑party libraries are prescribed except those dictated by the PRD:
  - Spring Boot (Web, Data JPA, Security, Test)
  - Hibernate Envers (audit)
  - Keycloak (OAuth2 Resource Server – Spring Security adapter)
  - Testcontainers (integration tests)
- Additional libraries (e.g., Lombok, MapStruct) may be introduced **only** if they clearly reduce boilerplate without compromising architectural purity. Each addition must be justified in an ADR (see §6.1).

### 3.3 Code Style
- Consistent formatting using Maven’s `formatter-maven-plugin` or the default IDE formatter (to be agreed once).
- No mandatory comment standard – but **public APIs** (controllers, domain public methods) should be self‑documenting via meaningful naming and, where beneficial, concise JavaDoc.

---

## 4. Git & Commit Conventions

### 4.1 Branching
- Main branch: `main` (production‑ready).
- One developer → no mandatory branch protection, but all work must be committed directly to `main` **only** after the local Definition of Done (DoD) is satisfied. If feature branches are used, they must be merged with a non‑fast‑forward merge.

### 4.2 Commit Messages
Follow **Conventional Commits** (v1.0.0):
```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```
- **Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `revert`
- **Scope**: module/component affected (e.g., `order-domain`, `security`, `infra-jpa`)
- **Subject**: imperative, lowercase, max 50 chars
- Example:  
  `feat(order-domain): add reservation timeout validation`

### 4.3 Manual Review Checklist (pre‑commit)
Before creating any commit, the developer must verify:
- [ ] All new domain logic is covered by unit tests.
- [ ] No infrastructure import leaks into the domain layer.
- [ ] The change passes `mvn clean verify` locally (including all tests).
- [ ] The commit message follows the conventional format.

---

## 5. Definition of Done (DoD)

A user story or task is **done** when all of the following are satisfied:

1. **Code**: All implementation code is written and follows the architectural principles (§2).
2. **Tests**:
   - Unit tests for all domain/application logic (≥ 95% line coverage on domain layer – hard gate).
   - Integration tests for repositories, Envers auditing, and scheduled jobs.
   - E2E smoke tests for critical workflows (place order → pay → ship → deliver) using Testcontainers.
3. **Documentation**:
   - OpenAPI/Swagger annotations are present on **all** REST endpoints (see §6.2).
   - Any architectural decision not already captured is recorded as a new ADR (see §6.1).
4. **Performance Benchmark** (non‑functional):  
   - The reservation timeout job must process 1,000 pending orders within 5 seconds (on standard hardware). This is validated by a dedicated performance test before release.
5. **Build**: The project compiles, and all tests (unit + integration) pass.
6. **Commit**: The changes are committed with a compliant message and the manual checklist (§4.3) is fully checked.

---

## 6. Documentation Standards

### 6.1 Architectural Decision Records (ADR)
- Every significant architectural, technological, or design decision must be documented as an ADR in the `/docs/adr/` directory.
- **Format**: `YYYY-MM-DD-short-title.md` (e.g., `2025-07-29-use-envers-for-audit.md`)
- **Template**:
  ```
  # Title
  
  ## Status
  [Proposed | Accepted | Deprecated]
  
  ## Context
  [Why are we making this decision?]
  
  ## Decision
  [What exactly is the decision?]
  
  ## Consequences
  [What trade-offs, risks, and benefits come with it?]
  ```
- Existing decisions from the PRD (e.g., Hexagonal architecture, Envers, Keycloak) should be recorded as the first ADRs at project kickoff.

### 6.2 API Documentation
- **All REST endpoints** must be annotated with **Swagger/OpenAPI 3** (`@Operation`, `@ApiResponse`, etc.).
- The generated OpenAPI document must be available at `/v3/api-docs` and the Swagger UI at `/swagger-ui.html`.
- DTOs must be described using `@Schema` annotations to clarify field meanings, constraints, and examples.

---

## 7. Testing Strategy & Quality Gates

### 7.1 Test Layers
| Layer | Mandatory | Tools |
|-------|-----------|-------|
| **Unit** | Yes – for all domain and application classes | JUnit 5, AssertJ, Mockito |
| **Integration** | Yes – repositories, Envers, scheduled jobs | Testcontainers (PostgreSQL) |
| **Controller (Web)** | Recommended – for request/response and security | `@WebMvcTest`, MockMvc |
| **E2E** | Yes – one full flow (place → pay → ship → deliver) | `@SpringBootTest` + Testcontainers |

### 7.2 Coverage Hard Gate
- **Domain layer** must have **≥ 95% line coverage** (verified by JaCoCo).
- Application layer ≥ 80% (recommended, not enforced).
- Other layers ≥ 50% (no hard gate, but encouraged).

### 7.3 Performance Benchmark (Non‑Functional)
- The scheduled job that cancels pending orders after 15 minutes must be benchmarked:
  - Given 1,000 orders in `PENDING` state, the job must cancel all of them within **5 seconds**.
- This test must be written as a separate performance integration test (using Testcontainers with a populated DB) and run before each release.

---

## 8. Release Process

Releases are tagged in the repository. Before creating a release:

### 8.1 Pre‑release Checklist
- [ ] All stories/tasks for the release are **Done** (DoD §5).
- [ ] The performance benchmark test passes.
- [ ] JaCoCo domain coverage ≥ 95% is confirmed.
- [ ] All ADRs are updated to reflect the final state of decisions.
- [ ] The `CHANGELOG.md` (at project root) is updated with the new version, following [Keep a Changelog](https://keepachangelog.com/).
- [ ] `pom.xml` version is incremented according to [Semantic Versioning](https://semver.org) (MAJOR.MINOR.PATCH).

### 8.2 Release Execution
1. Commit the version bump and changelog update with `chore(release): vX.Y.Z`.
2. Create a Git tag: `git tag -a vX.Y.Z -m "Release vX.Y.Z"`.
3. Push the tag to the remote.
4. Build the JAR: `mvn clean package`.
5. (Optional) Archive the JAR and attach it to the release notes.

---

## 9. Manual Review Checklist (Pre‑commit & PR)

Although there is only one developer, every commit must satisfy the following before it is pushed:

- [ ] **Architecture**: No violation of Hexagonal layering (domain independent of Spring/JPA).
- [ ] **Tests**: New/changed logic has corresponding unit/integration tests.
- [ ] **Coverage**: Domain coverage remains ≥ 95% (run `mvn jacoco:report`).
- [ ] **Commit message**: Follows Conventional Commits.
- [ ] **Swagger**: New/updated endpoints include OpenAPI annotations.
- [ ] **Documentation**: ADR created if a significant decision was made.
- [ ] **Build**: `mvn clean verify` passes without errors.
- [ ] **Performance**: If the reservation job was touched, the performance benchmark still passes.

---

## 10. Non‑Negotiable Rules (The “Constitution”)
1. The domain layer **must never** contain Spring annotations (no `@Component`, `@Service`, `@Transactional`, etc.).
2. All state mutations on an `Order` must go through the aggregate root; no setter‑based updates from services.
3. The reservation timeout (15 minutes) must be validated by the domain (`Order.isReservationExpired()`) – the scheduled job merely calls that method.
4. All infrastructure implementations (JPA repositories, Envers, Keycloak) are replaceable – the domain depends only on interfaces.
5. Every release must include updated ADRs and CHANGELOG.

---

*This Constitution is effective immediately and supersedes any prior ad‑hoc practices. Any amendment requires updating this document and recording a corresponding ADR.*