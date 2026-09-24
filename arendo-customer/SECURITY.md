# Security policy

## Reporting

Do not publish credentials or device tokens in a GitHub issue. Report suspected access, replay, signature, update or device-identity vulnerabilities through the private project channel.

## Repository rules

- Never commit production device tokens or technician credentials.
- Never log access grants, private keys or complete authorization headers.
- Device permissions must be scoped to explicit postamat identifiers and expiry time.
- Captured USB traffic must not be reusable as a new authorization.
- State-changing commands require unique identifiers and expiry checks.
- Update packages require integrity and authenticity verification.

The current source is a development foundation and has not completed a production security review.

