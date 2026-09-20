#!/usr/bin/env sh
set -eu

mkdir -p backups
chmod 700 backups 2>/dev/null || true

api_id="$(docker compose ps -q api)"
if [ -z "$api_id" ]; then
  echo "Swami Bags API container is not running."
  echo "Start the stack first with: docker compose up -d"
  exit 1
fi

timestamp="$(date +%Y%m%d-%H%M%S)"
archive="swami-data-$timestamp.tgz"

echo "Pausing the private admin service for a consistent SQLite backup..."
docker compose stop api >/dev/null
restart_api() {
  docker compose start api >/dev/null 2>&1 || true
}
trap restart_api EXIT INT TERM

docker compose --profile ops run --rm -T -e "ARCHIVE=$archive" ops   sh -c 'tar czf "/backup/$ARCHIVE" -C /data .'

docker compose start api >/dev/null
trap - EXIT INT TERM

chmod 600 "backups/$archive" 2>/dev/null || true
echo "Backup written to backups/$archive"
echo "The public Nginx catalogue remains available while the admin service is paused."
