# Keycloak-backed Identity Strategy

## Status
Superseded by [2026-08-24-self-issued-jwt-with-persistent-accounts.md](2026-08-24-self-issued-jwt-with-persistent-accounts.md). Keycloak remains an optional future migration path for SSO/MFA/federation; it is not part of the current implementation.

## Context
The application must expose secure role-aware endpoints while keeping the security model aligned with modern OAuth2 resource-server practices.

## Decision
Adopt a Keycloak-backed OAuth2 resource-server model for future identity and authorization verification, while keeping the current test and controller security path aligned with the active Spring Security configuration.

## Consequences
Benefits:
- standards-based identity integration
- role mapping and token-based access control
- future readiness for enterprise identity expansion

Trade-offs:
- requires environment alignment for key issuer and JWT validation details
- current repository state uses the verified in-memory role-based test path until full issuer wiring is completed
