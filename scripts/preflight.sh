#!/usr/bin/env sh
set -eu

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

warn() {
  echo "WARNING: $*" >&2
}

[ -f .env ] || fail ".env is missing. Run: cp .env.example .env"

command -v docker >/dev/null 2>&1 || fail "Docker is not installed or not in PATH."
docker compose version >/dev/null 2>&1 || fail "Docker Compose plugin is unavailable."

read_env() {
  key="$1"
  value="$(grep -E "^$key=" .env | tail -n 1 | cut -d= -f2- || true)"
  printf '%s' "$value"
}

admin_user="$(read_env ADMIN_USERNAME)"
admin_password="$(read_env ADMIN_PASSWORD)"
whatsapp="$(read_env WHATSAPP_NUMBER)"
public_url="$(read_env PUBLIC_BASE_URL)"
openai_key="$(read_env OPENAI_API_KEY)"

[ -n "$admin_user" ] || fail "ADMIN_USERNAME is empty."
[ "${#admin_password}" -ge 12 ] || fail "ADMIN_PASSWORD must be at least 12 characters."
case "$admin_password" in
  *CHANGE_ME*|*change_me*) fail "Replace the placeholder ADMIN_PASSWORD." ;;
esac

if [ -z "$whatsapp" ] || printf '%s' "$whatsapp" | grep -q '98765'; then
  warn "WHATSAPP_NUMBER still looks empty or like a placeholder."
fi

if [ -z "$public_url" ] || printf '%s' "$public_url" | grep -q 'localhost'; then
  warn "PUBLIC_BASE_URL is not set to the final public URL yet."
fi

if [ -z "$openai_key" ]; then
  warn "OPENAI_API_KEY is empty. The site will work, but AI marketing-image generation will be disabled."
fi

docker compose config --quiet
echo "Preflight passed."
