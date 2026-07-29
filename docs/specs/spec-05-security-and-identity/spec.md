# Spec 5: Security & Identity

## 1. Spec Metadata
- **Name**: Security & Identity
- **Dependencies**: Spec 2, Spec 6
- **Estimated Effort**: Medium

## 2. In-Scope Artifacts
After this spec is complete, the following artifacts should exist:

- Package: `com.example.inventory.infrastructure.security`
  - `SecurityConfig`
  - `JwtAuthenticationConverter`
  - `KeycloakProperties`

## 3. Core Domain Models & Contracts

### Security Roles
- `ADMIN`
- `WAREHOUSE`
- `CUSTOMER`

### Access Rules
- Admin endpoints may manage inventory and order state.
- Warehouse users may update shipping/delivery state.
- Customers may view and manage their own orders.

## 4. Behavioral Specifications
1. JWTs issued by Keycloak are validated by the application.
2. Requests without valid tokens are rejected.
3. Method-level authorization ensures only permitted roles access certain operations.

## 5. Input / Output Contracts
- Security is enforced at the controller and handler level.
- No DTO changes are required here beyond authentication context handling.

## 6. Technical Constraints / Non-Functional Rules
- The application should support OAuth2 Resource Server configuration.
- The domain layer must remain free from security annotations.
- Local development should support either a mock JWT provider or a test container.

## 7. Acceptance Criteria & Test Matrix
1. Given an unauthenticated request, when it hits a protected endpoint, then it is rejected.
2. Given a customer token, when it calls an admin endpoint, then access is denied.
3. Given an admin token, when it updates inventory, then access is allowed.
