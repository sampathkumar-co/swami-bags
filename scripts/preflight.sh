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
session_secure="$(read_env SESSION_COOKIE_SECURE)"
web_bind="$(read_env WEB_BIND_ADDRESS)"
whatsapp="$(read_env WHATSAPP_NUMBER)"
public_url="$(read_env PUBLIC_BASE_URL)"
openai_key="$(read_env OPENAI_API_KEY)"

[ -n "$admin_user" ] || fail "ADMIN_USERNAME is empty."
[ "${#admin_password}" -ge 20 ] || fail "ADMIN_PASSWORD must be at least 20 characters for production."
case "$admin_password" in
  *CHANGE_ME*|*change_me*) fail "Replace the placeholder ADMIN_PASSWORD." ;;
esac

case "$public_url" in
  https://*)
    [ "$session_secure" = "true" ] || fail "SESSION_COOKIE_SECURE must be true when PUBLIC_BASE_URL uses HTTPS."
    ;;
esac

case "${web_bind:-127.0.0.1}" in
  127.0.0.1|::1|localhost) ;;
  *) fail "WEB_BIND_ADDRESS must stay loopback-only; put the public HTTPS reverse proxy in front of it." ;;
esac

if command -v stat >/dev/null 2>&1; then
  env_mode="$(stat -c '%a' .env 2>/dev/null || true)"
  case "$env_mode" in
    600|400) ;;
    "") warn "Could not determine .env file permissions." ;;
    *) fail ".env permissions are $env_mode; run: chmod 600 .env" ;;
  esac
fi

if [ -z "$whatsapp" ] || printf '%s' "$whatsapp" | grep -q '98765'; then
  warn "WHATSAPP_NUMBER still looks empty or like a placeholder."
fi

if [ -z "$public_url" ] || printf '%s' "$public_url" | grep -Eq 'localhost|your-domain\.example'; then
  warn "PUBLIC_BASE_URL is not set to the final public URL yet."
fi

if [ -z "$openai_key" ]; then
  warn "OPENAI_API_KEY is empty. The site will work, but AI marketing-image generation will be disabled."
fi

docker compose config --quiet
echo "Preflight passed."
