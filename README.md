# Swami Bags

A light, professional wholesale bag catalogue with a static-first public website and a private Spring Boot admin workflow.

## What is implemented

### Public website

- Home, catalogue, product detail, About and Contact
- Cash Bags, Luggage Bags, Jute Bags, Zip Bags and Purses
- wholesale price, MOQ, material, stock and restock information
- responsive light cream / burgundy visual system
- product search and category filtering
- quantity-aware WhatsApp enquiry messages
- generic wholesale enquiry builder
- runtime catalogue snapshot; demo data is development-only and never shown as real production stock

### Private admin

- session-protected admin login
- create/edit/delete products
- update price, MOQ, stock and restock time
- upload real JPEG/PNG product photos with strict decode, dimension, sanitizing re-encode and total-count validation
- automatically generate a marketing draft after photo upload when OpenAI is configured
- review/regenerate/approve generated marketing visuals
- publish/unpublish products without editing source code
- dashboard for product/publish/stock/image state
- editable brand, WhatsApp, phone, email, address and public-site settings

### AI image workflow

The server sends the real product references to the OpenAI Images API with a fidelity-focused prompt. The AI is deliberately told **not to generate product text**. After image generation, the Spring Boot service places verified product information on the visual itself so the catalogue does not depend on AI-generated spelling, prices or specifications.

Generated images are drafts until an admin explicitly approves them. Transient image-API failures are retried with bounded backoff; failures remain recorded in generation history for diagnosis.

### Low-load production architecture

```text
Nginx
├── React/Vite static files
├── /catalog/*.json
├── /media/*
└── /api/* → Spring Boot admin service
              └── SQLite + OpenAI
```

Public browsing is served by Nginx. Spring Boot/SQLite are only involved in admin actions, so the VPS does not do application work for every catalogue visitor.

## Stack

- React 19 + Vite + TypeScript
- React Router
- Spring Boot 4 / Java 21
- Spring Security
- SQLite
- OpenAI Images API
- Nginx
- Docker Compose
- GitHub Actions

## Local frontend checks

```bash
npm ci
npm run lint
npm run build
```

## Server checks

```bash
cd server
mvn test package
```

## Production

See [DEPLOYMENT.md](DEPLOYMENT.md).

The repository includes:

- `Dockerfile` for the static web container
- `server/Dockerfile` for Spring Boot
- `docker-compose.yml`
- production `nginx.conf`
- `.env.example`
- GitHub Actions CI
- CodeQL static security analysis and Dependabot
- end-to-end production Docker/security/backup-restore smoke tests
- `scripts/backup.sh` for consistent persistent-data backups

## Final inputs needed before going live

The app is intentionally configured with placeholders for client-specific inputs. Before launch provide the real:

- WhatsApp / phone number
- logo / brand details if they change
- business address/email
- product photos and product data
- OpenAI API key
- VPS/domain/HTTPS details

Do not put real secrets in Git.
