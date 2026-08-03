# Implementation Plan for Spec 5

This plan breaks Spec 5 into concrete implementation phases so the security and identity work can be built incrementally while staying aligned with the repository’s current Spring Boot architecture.

## Constitutional Guardrails
- The domain layer must remain free of security annotations and auth-specific logic.
- Security concerns belong in the infrastructure layer and the web layer.
- Authorization decisions should be enforced at the controller and handler boundaries.
- The implementation must be verifiable with Spring Security tests and integration tests.

---

## Phase 1 — Establish the security foundation

### Goal
Create the base security configuration and wire the application into Spring Security OAuth2 Resource Server support.

### Tasks
1. Add the required Spring Security OAuth2 Resource Server dependencies if they are not already present.
2. Create the security infrastructure package under com.example.inventory.infrastructure.security.
3. Implement SecurityConfig with:
   - stateless session management
   - JWT resource server configuration
   - a basic authorization policy for protected endpoints
4. Add configuration properties for Keycloak or a local JWT issuer so the application can be started in development.
5. Ensure unauthenticated requests are rejected for protected routes by default.

### Deliverables
- SecurityConfig
- Initial JWT-based authentication flow
- Protected endpoint baseline

---

## Phase 2 — Add JWT claim mapping and role handling

### Goal
Translate JWT claims into application roles and make them available to Spring Security authorizations.

### Tasks
1. Implement JwtAuthenticationConverter to map JWT claims to Spring GrantedAuthority values.
2. Define the expected role claims and align them with the spec roles: ADMIN, WAREHOUSE, CUSTOMER.
3. Configure the converter so role-based access decisions can be applied consistently.
4. Add a properties class for Keycloak settings, such as issuer URI and expected audience.
5. Ensure role mapping is testable without coupling it to the domain layer.

### Deliverables
- JwtAuthenticationConverter
- KeycloakProperties
- Role mapping from JWT claims to authorities

---

## Phase 3 — Apply method and endpoint authorization rules

### Goal
Enforce the access rules defined in the spec at the API boundary.

### Tasks
1. Add role-based authorization rules for admin inventory and order management operations.
2. Add role-based authorization rules for warehouse shipping and delivery state updates.
3. Add role-based authorization rules for customer self-service order access and management.
4. Apply authorization at the controller or service boundary so the domain layer stays clean.
5. Use method security annotations where appropriate to enforce behavior consistently.

### Deliverables
- Authorization rules for admin, warehouse, and customer flows
- Protected controller and handler boundaries
- Clear separation between role-based access controls and domain logic

---

## Phase 4 — Add authentication and authorization tests

### Goal
Verify the security behavior described in the acceptance criteria.

### Tasks
1. Add tests for unauthenticated requests to protected endpoints being rejected.
2. Add tests proving customer tokens cannot access admin-only endpoints.
3. Add tests proving admin tokens can access inventory update operations.
4. Add a local test setup using mock tokens or a test container for JWT validation.
5. Keep the tests focused on security behavior rather than business logic.

### Deliverables
- Security integration tests
- Coverage for the three acceptance criteria
- Confidence that authentication and authorization behave as expected

---

## Phase 5 — Validate the implementation and finalize the security layer

### Goal
Ensure the security layer is consistent, maintainable, and ready for the next spec.

### Tasks
1. Review the configuration naming and property handling for clarity.
2. Confirm that no domain classes depend on security annotations.
3. Run the Maven test suite and confirm the new security tests pass.
4. Review the web layer to ensure protected endpoints and public endpoints are clearly separated.
5. Prepare the implementation for integration with the upcoming web interface and OpenAPI work.

### Deliverables
- Completed security and identity infrastructure layer
- Passing Maven test suite with security coverage
- Clean and maintainable authentication and authorization setup

---

## Definition of Done for Spec 5
- The infrastructure security package contains the required classes.
- JWT-based authentication is configured and validated.
- Role-based authorization is enforced for admin, warehouse, and customer access rules.
- Unauthenticated requests are rejected and unauthorized role access is denied.
- Maven tests pass, including the new security coverage.
