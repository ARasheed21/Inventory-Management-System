# AGENT.md — Rules for AI Agents Working on This Project

Rules any agent (or human) must follow when implementing features or refactoring in this
repository. Violating these has caused real wasted cycles in past sessions; each rule exists
for a reason. Deeper background lives in `docs/development-pitfalls.md` and
`docs/constitution.md`.

## 1. Project snapshot

- Spring Boot 3.5 / Java 21 modular monolith: `domain` → `application` → `infrastructure` → `web`
- PostgreSQL (runtime) / H2 (tests); JWT auth; STOMP WebSocket; OpenAPI via springdoc
- Current state: all 10 PRD gaps resolved, ~74 green tests. Do not regress these.
- PRD user stories are the source of truth (`docs/prd.md`); compliance status in
  `docs/prd-compliance-gap-report.md`

## 2. Workflow (mandatory)

1. **Plan before code**: confirm scope, endpoint contract changes, and the exact behavior list
   to test with the user before writing the first test.
2. **TDD, vertical slices**: one RED test -> minimal GREEN -> next. Never write all tests first.
3. **Unit-test pure logic first** (validators, limiters, mappers, guards): plain JUnit against the
   class, then wire and add an integration test. Integration boots cost ~20s; unit tests ~ms.
4. **Never refactor while RED.**
5. After finishing: run the full suite, update docs (section 6), propose a commit message.

## 3. Build & verification hygiene

- Incremental compilation here is unreliable: after editing `src/main`, use
  `mvn clean test -Dtest=X` (not plain `mvn test`).
- Never suppress Maven output while iterating; compile errors have been misread as test failures.
- Identical `Time elapsed` across runs = stale surefire report; suspect staleness before logic.

## 4. Architecture rules (hexagonal / DDD / CQRS)

- Application handlers depend only on ports (`application/ports`) and domain repository
  interfaces — never on infrastructure classes directly. Add an adapter instead
  (pattern: `PaymentFailureNotifier` port -> `WebSocketPaymentFailureNotifier` adapter).
- Domain state machine lives in `OrderStatus.canTransitionTo`; never bypass it.
- New write flows = command + handler under `application`; new reads = query + handler.
- WebSocket pushes go through `OrderWebSocketService`: broadcast `/topic/orders/{id}`,
  private `/user/queue/orders`. Document new payload shapes in `docs/api-contract/asyncapi-ws.md`.

## 5. API & contract-first rules

- **Any REST change requires regenerating the committed contract**: run
  `mvn test -Dtest=OpenApiContractExportTest` and commit `docs/api-contract/openapi.yaml`
  together with the code change.
- Annotate every controller with `@Tag`/`@Operation`/`@ApiResponse` and every DTO field with
  `@Schema` (including validation constraints). Undocumented endpoints are a defect.
- Frontends are driven by the contract, not by reading code. Breaking contract changes must be
  flagged in the changelog.
- Error responses always use the standard body (`ApiError` schema): timestamp/status/error/
  message/path. Use `ResourceNotFoundException` for missing entities (404), let domain
  `IllegalStateException` surface as 409.

### Exception -> HTTP status mapping (do not break)

| Exception | Status |
|---|---|
| MethodArgumentNotValidException | 400 |
| IllegalArgumentException | 400 |
| IllegalStateException | 409 |
| ResourceNotFoundException | 404 |
| AccessDeniedException | 403 |
| AuthenticationException | 401 |

- Gotcha: the blanket `Exception` handler swallows Spring Security exceptions unless dedicated
  handlers exist. New security mechanisms require rechecking this mapping.

## 6. Security rules

- Every new endpoint needs explicit authorization (URL rule in `SecurityConfig` or
  `@PreAuthorize`). Default is authenticated; admin-only areas additionally guarded.
- Roles: ADMIN > WAREHOUSE > CUSTOMER. Customers are always scoped to their own data via
  ownership checks; staff endpoints that need cross-customer visibility must be explicit
  (see `/api/fulfillment/orders`).
- Password policy and login rate limiting exist — do not weaken them; extend them for new
  credential surfaces.
- Never reintroduce the default `jwt.secret` under prod profiles; `JwtSecretGuard` fails startup
  by design.

## 7. Testing conventions

- Seeded accounts (from `UserRegistry.seedAccounts`): `admin/admin`, `warehouse/warehouse`,
  `customer/customer`. Helper: `TestAuthHelper.obtainAccessToken(...)`.
- SQL fixtures live in `src/test/resources/*-test-data.sql` using the DELETE+INSERT pattern;
  create a new fixture per feature, do not mutate shared ones mid-suite.
- H2 timezone skew: seeded timestamps read back shifted (~+3h). For "expired/past" data seed
  >= 4 hours back (`DATEADD('MINUTE', -240, CURRENT_TIMESTAMP)`).
- Ids are server-generated UUIDs — always capture ids from create responses; never assume
  client-chosen ids work.
- Derive expected values from domain rules (e.g., revisions count = number of state transitions),
  don't guess.
- State preconditions of every test explicitly (which account owns what, which fixture ran).

## 8. Persistence gotchas

- APIs expose external ids (`external_id` columns); internal Long ids are JPA/Envers keys.
- Envers queries return `Object[] {entity, AuditRevisionEntity, RevisionType}` tuples — not lists.
- Audited entities: Order, OrderItem, Product (`products_aud`, `orders_aud`, `revinfo` tables;
  both H2 schemas must stay in sync when adding tables/columns).

## 9. Documentation duties (part of Definition of Done)

- Feature work: update `README.md` (features/endpoints/config/test count) and add a `CHANGELOG.md`
  entry under `[Unreleased]`.
- Significant architectural/design decisions: add an ADR in `docs/adr/`
  (`YYYY-MM-DD-short-title.md`, Status/Context/Decision/Consequences). Mark superseded ADRs
  with a link to their successor.
- Discovered project quirks: add them to `docs/development-pitfalls.md` so nobody rediscovers them.
- PRD-visible behavior changes: update `docs/prd-compliance-gap-report.md` if it affects gap status.

## 10. Definition of Done

- [ ] Full suite green from a clean build (`mvn clean test`)
- [ ] `openapi.yaml` regenerated if any API surface changed
- [ ] New endpoints annotated, authorized, and covered by integration tests through public interfaces
- [ ] Docs updated per section 9
- [ ] Commit message follows conventional commits (`feat:`, `fix:`, `chore:`, ...) with a body
      explaining why
