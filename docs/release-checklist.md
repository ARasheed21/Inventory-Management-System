# Release Checklist

## Pre-release verification
- [x] All required implementation specs are reflected in the repository.
- [x] The performance benchmark test is present and passing.
- [x] Maven verification has been run successfully.
- [x] ADRs exist for the major architectural decisions.
- [x] The changelog has been updated for the release scope.
- [x] The release metadata is consistent with the current repository state.

## Release execution
1. Confirm `mvn verify` is green.
2. Confirm the benchmark and integration suites are green.
3. Update the release version in Maven metadata intentionally.
4. Commit the version bump and documentation update.
5. Create the release tag.
6. Push the tag to the remote repository.
7. Build the final JAR for publication.
