# Contributing

## Workflow

1. Open an issue describing the requirement, defect or proposed architecture change.
2. Reference relevant requirement IDs such as `A04`, `C03` or acceptance tests such as `T14`.
3. Keep protocol changes versioned and backward-compatible or document the migration.
4. Add or update tests for behavior changes.
5. Do not mix customer UI styling changes with security or protocol changes in one pull request.

## Pull-request checklist

- No secrets or personal data are included.
- Build instructions still work on a clean machine.
- State-changing commands remain idempotent.
- Offline and reconnect behavior is documented.
- Relevant acceptance rows in `docs/testing/acceptance-matrix.md` are updated.

