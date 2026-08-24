# Changelog

All notable changes to this project will be documented in this file.

## [0.1.0] - 2026-08-24

### Added
- Self-service customer registration (`POST /auth/register`) backed by database-persisted accounts with BCrypt password hashes; duplicate signup returns 409.
- Warehouse fulfillment endpoints (`POST /api/orders/{id}/ship`, `/deliver`) restricted to WAREHOUSE/ADMIN roles, with invalid lifecycle transitions returning 409.
- Server-computed reservation countdown (`reservationSecondsRemaining`) on every order response and on the order-status endpoint.
- WebSocket push notifications: `RESERVATION_EXPIRED` when the timeout job cancels orders, and `PAYMENT_FAILED` (with reason) via a `PaymentFailureNotifier` application port when payment cannot proceed.
- Payment guard: paying an order past its reservation window fails with 409 instead of silently succeeding.
- Admin audit-history endpoints (`GET /api/admin/audit/products/{id}`, `/orders/{id}`) exposing Envers revisions with author, timestamp, revision type, and field snapshots.
- Product auditing via Envers (`@Audited` on `ProductJpaEntity`, `products_aud` table).
- Reserved-inventory report (`GET /api/inventory/reserved`) aggregating pending-order allocations per product for warehouse/admin users.
- Security hardening: startup guard refusing default `jwt.secret` under production profiles, password policy on registration, per-username login rate limiting (`429` after threshold failures).

### Changed
- User registry migrated from in-memory catalog to database-backed accounts seeded at startup (admin/warehouse/customer).
- Global exception handler now maps `AccessDeniedException` to 403, `AuthenticationException` to 401, and `IllegalStateException` to 409.
- Missing-entity handlers (payment, ship, deliver) return 404 via `ResourceNotFoundException`.
- Order repository bulk cancellation returns cancelled orders so the timeout job can notify owners.

### Verified
- Full suite green: 72 tests (unit, integration, STOMP WebSocket, security).
- All ten PRD compliance gaps closed; see `docs/prd-compliance-gap-report.md`.

## [Unreleased]

### Added
- Completed the web API and OpenAPI transport boundary for order management.
- Added controller integration coverage for API contracts and security posture.
- Added a deterministic end-to-end order lifecycle regression test.
- Added a performance benchmark proof for pending-order timeout cancellation.
- Added repository bulk cancellation support to improve timeout-job throughput.

### Changed
- Aligned the release documentation package with the verified implementation baseline.
- Added release governance artifacts and ADR structure for final publication readiness.

### Verified
- Maven verification passes with the repository-wide test suite green.
- Benchmark proof validates that the timeout path processes pending reservations within the required threshold.
