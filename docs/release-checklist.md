# Release Checklist

Maintainer-facing gates for publishing a release. Every item must be checked before tagging.

## 1. Verification gates

- [ ] `mvn clean verify` passes from a clean working tree (no stale build artifacts)
- [ ] Full test suite is green (current baseline: 72 tests)
- [ ] Performance benchmark (`ReservationTimeoutBenchmarkTest`) passes within threshold
- [ ] No compilation warnings introduced in changed modules

## 2. Documentation artifacts

- [ ] `CHANGELOG.md` has a section for the release version with Added/Changed/Verified entries
- [ ] `docs/adr/` contains ADRs for all significant decisions made since the last release
- [ ] Any superseded ADRs are marked as such and link to their successor
- [ ] `README.md` reflects current features, endpoints, configuration, and test count
- [ ] `docs/prd-compliance-gap-report.md` is current (or states no open gaps)
- [ ] Swagger annotations are present on any new/changed endpoints

## 3. Security & configuration review

- [ ] `JWT_SECRET` default is safe for the target profile (startup guard enforces this under
      prod/production — verify the guard test passes: `JwtSecretGuardTest`)
- [ ] New properties are documented in README and `.env.example` if applicable
- [ ] New endpoints have explicit authorization rules (URL rule or `@PreAuthorize`) with tests

## 4. Versioning & publication sequence

1. Bump `<version>` in `pom.xml` to the release version (remove `-SNAPSHOT`).
2. Update `CHANGELOG.md` heading from `[Unreleased]` content into `[x.y.z] - YYYY-MM-DD`.
3. Run `mvn clean verify` one final time; record the result.
4. Commit the release metadata changes.
5. Tag: `git tag -a vx.y.z -m "Release x.y.z"` and push with `--follow-tags`.
6. Record the final commit SHA and tag in the changelog entry.

## Handoff

A second maintainer (or a fresh reviewer) reviews the diff between the previous release tag and
this one, confirms every checklist box, then approves publication.
