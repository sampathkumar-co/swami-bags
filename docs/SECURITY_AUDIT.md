# Swami Bags Security Audit

Audit date: 2026-09-20

Scope: React/Vite frontend, Spring Boot admin API, authentication/session handling, CSRF, uploads/media, SQLite, AI generation, Nginx, Docker Compose, CI/CD, dependencies, secrets, backup/restore and deployment controls.

## Confirmed controls

- Normal catalogue traffic is static-first and does not query the private DB/API.
- `/api/admin/**` requires ADMIN authentication except login/CSRF bootstrap.
- State-changing admin requests are CSRF protected.
- BCrypt password hashing; unsafe placeholder/short production passwords are rejected.
- SQL uses parameterized Spring JDBC.
- OpenAI key remains server-side and provider requests use a hard-coded HTTPS origin.
- API has no published host port.
- Login and AI generation are separately rate-limited.
- Product uploads are actually decoded, dimensions are bounded, and images are re-encoded before publication.
- Public media paths are normalized and randomized.
- Browser security headers/CSP, log rotation, backup and restore procedures are present.
- CI actions are immutable-SHA pinned; npm audit, CodeQL and Dependabot provide continuing checks.

## Findings remediated

1. Admin session reduced from 8 hours to 30 minutes.
2. CSRF cookie made HttpOnly; client gets the token from the dedicated endpoint.
3. Explicit admin session cookie name, cookie-only tracking and fixation protection.
4. Detailed framework error/stacktrace exposure disabled.
5. Actuator reduced to health only, without details.
6. Secure-cookie and loopback-only production defaults.
7. MIME spoofing, oversized/decompression-bomb and metadata/polyglot upload risks reduced by decode/limits/re-encode.
8. Stronger CSP, anti-framing, HSTS, slow-client and connection controls.
9. Separate throttle for paid AI generation.
10. API container read-only root FS, dropped Linux capabilities and no-new-privileges.
11. CI checks security headers, auth cookie flags and malicious fake-image rejection.
12. GitHub Actions upgraded/pinned; CodeQL and Dependabot added.

## Live-only release gates

Must be tested after the real VPS/domain exists: TLS certificate/redirect/ciphers, DNS and origin exposure, external port scan, firewall, SSH policy, OS/Docker patch level, CDN/WAF configuration, real `.env` permissions, backup restore drill, production security-header scan, live rate limiting behind the real proxy, and OpenAI billing/key-rotation controls.

## Repository governance gate

At audit time `main` was unprotected and no ruleset existed. Protect `main` and require CI + CodeQL checks so direct pushes cannot bypass the release gates.

## Residual risks

- The web container receives the shared `/data` volume read-only, including SQLite; it is not URL-routable, but splitting private DB data from public catalog/media would further reduce blast radius.
- One shared admin account is appropriate only for a single operator; multiple operators should get individual accounts, MFA and audit logging.
- CSP still permits inline CSS for the current UI; scripts remain restricted to self.
- Backup archives are not automatically encrypted; protect offsite copies appropriately.
