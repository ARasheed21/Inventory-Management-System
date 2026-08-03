# Changelog

All notable changes to this project will be documented in this file.

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
