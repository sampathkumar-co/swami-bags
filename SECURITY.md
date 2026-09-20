# Security Policy

Security fixes apply to the current `main` branch and the production deployment built from it.

## Reporting

Do not put credentials, API keys, private customer/business data or exploit payloads in a public issue. Report sensitive findings privately to the repository owner and rotate any possibly exposed credential first.

## Production requirements

- HTTPS at the trusted public edge.
- `SESSION_COOKIE_SECURE=true`.
- `WEB_BIND_ADDRESS=127.0.0.1` when a host reverse proxy terminates HTTPS.
- Never publish the Spring Boot `api` container directly.
- Use a unique random admin password (20+ characters recommended).
- Keep the real `.env` out of Git and permission-restricted.
- Keep OpenAI credentials server-side with billing/usage alerts.
- Require CI/CodeQL before production changes.
- Copy backups off the VPS and periodically restore-test them.

## Implemented controls

Spring Security ADMIN authorization, BCrypt, SameSite/HttpOnly cookies, CSRF protection, Nginx login and AI-generation throttles, CSP/security headers, strict image decoding/re-encoding, parameterized SQL, Docker network isolation and privilege reduction, npm audit, CodeQL and Dependabot.
