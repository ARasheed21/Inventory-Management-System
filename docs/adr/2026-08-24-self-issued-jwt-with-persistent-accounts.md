# Self-Issued JWT with Persistent Accounts

## Status
Accepted — supersedes [2026-08-03-keycloak-backed-identity-strategy.md](2026-08-03-keycloak-backed-identity-strategy.md)

## Context
The PRD requires role-aware security with customer self-service. The original ADR planned a
Keycloak-backed OAuth2 resource server. During PRD gap analysis (gap 8) we found that Keycloak
integration carries an external-infrastructure cost that outweighed its benefit for this release:
the application only needs registration, login/refresh, and DB-driven role authorization, all of
which can be satisfied without an external identity provider. Meanwhile, real deployment risks
existed in the self-managed path (default JWT secret forgeable by anyone, no brute-force
resistance, weak password acceptance).

## Decision
Implement identity in-application:
- accounts persisted in a database `accounts` table with BCrypt password hashes
- self-issued JWTs (HS256 default, RS256 configurable) via `/auth/register`, `/auth/login`,
  `/auth/refresh`, `/auth/me`
- roles stored per account and embedded in tokens; authorization enforced via URL rules and
  `@PreAuthorize`
- hardening guards: startup refusal on default `jwt.secret` under production profiles, password
  policy (8+ chars, letter + digit), per-username login rate limiting returning 429

Keycloak remains an optional future migration for SSO/MFA/federation needs; it is not required
for PRD compliance.

## Consequences
Benefits:
- zero external infrastructure for identity; simpler local development and CI
- full control over account model and audit of authentication events
- all PRD security behaviors covered and tested (72-test suite includes STOMP-authenticated flows)

Trade-offs:
- no SSO/MFA/federation; each additional client must use this app's token endpoint
- no token revocation (stateless JWTs) — stolen access tokens are valid until expiry; mitigated
  by short access-token lifetime (900s)
- credential storage, password policy, and brute-force protection are owned by this codebase
  rather than delegated to an identity provider
- migrating to Keycloak later will require frontend auth-layer changes (token issuer/claims)
