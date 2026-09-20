# Swami Bags — VPS Deployment

The production design keeps public traffic lightweight:

```text
Browser
   │
   ▼
Nginx static frontend
   ├── /assets/*   → hashed React assets
   ├── /catalog/*  → generated public JSON from shared volume
   ├── /media/*    → product/marketing images from shared volume
   └── /api/*      → Spring Boot (admin only)
                        │
                        ├── SQLite
                        └── OpenAI image generation
```

Normal catalogue browsing does **not** query Spring Boot or SQLite. Spring Boot is used for the private admin workflow and writes a small public catalogue snapshot that Nginx serves directly.

## 1. VPS prerequisites

Install:

- Git
- Docker Engine
- Docker Compose plugin

The deployment only exposes the Nginx/web port. The Spring Boot service stays inside the Compose network.

## 2. Clone and configure

```bash
git clone https://github.com/sampathkumar-co/swami-bags.git
cd swami-bags
cp .env.example .env
nano .env
```

Before launch, set at minimum:

```dotenv
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<long-random-password-that-is-not-the-placeholder>
WHATSAPP_NUMBER=91XXXXXXXXXX
BUSINESS_PHONE=+91 XXXXXXXXXX
BRAND_NAME=Swami Bags
PUBLIC_BASE_URL=https://your-domain.example
```

For AI marketing-image generation also set:

```dotenv
OPENAI_API_KEY=<server-side-api-key>
OPENAI_IMAGE_MODEL=gpt-image-2.5-sunburst
OPENAI_IMAGE_QUALITY=medium
OPENAI_IMAGE_SIZE=1536x1024
```

Never commit the real `.env`.

## 3. Start

```bash
docker compose up -d --build
docker compose ps
```

Default public port:

```text
http://VPS-IP:8088
```

Change `WEB_PORT` in `.env` if required.

## 4. Verify

```bash
curl -fsS http://127.0.0.1:8088/healthz
curl -fsS http://127.0.0.1:8088/api/admin/auth/csrf
docker compose ps
docker compose logs --tail=100 api
docker compose logs --tail=100 web
```

Then open:

```text
http://VPS-IP:8088/admin/login
```

The intended first-product flow is:

```text
Sign in
→ Add product details
→ Save
→ Upload real product photos
→ AI marketing draft generates automatically when the API key is configured
→ Review / regenerate if needed
→ Approve
→ Publish
→ Public /catalog/products.json updates automatically
```

## 5. HTTPS / domain

When a real domain is connected, terminate HTTPS at the existing reverse proxy or Cloudflare setup and forward to the web container.

After HTTPS is working set:

```dotenv
SESSION_COOKIE_SECURE=true
PUBLIC_BASE_URL=https://your-domain.example
```

Do not expose the Spring Boot container directly to the internet.

## 6. Persistent data

All changing data lives in the Docker volume attached at `/data`:

```text
/data/swami-bags.db
/data/media/
/data/catalog/
```

The SQLite database uses a single Hikari connection to keep resource use low.

## 7. Backup

A backup script is included. It briefly pauses only the private API so the SQLite snapshot is consistent; Nginx keeps serving the public catalogue and images.

```bash
sh scripts/backup.sh
```

Archives are written to `./backups/`. Copy important backups off the VPS as well.

## 8. Updating production

```bash
git pull --ff-only
docker compose up -d --build
docker image prune -f
```

The named data volume is not replaced by application rebuilds.

## 9. Things intentionally left for final deployment

These are environment/business inputs rather than missing application architecture:

- final WhatsApp and phone numbers
- logo and real product photographs
- OpenAI API key
- final domain / DNS
- HTTPS termination
- final business address/email
- first real product catalogue data

Everything else should remain reproducible from the repository.
