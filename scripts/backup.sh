#!/usr/bin/env sh
set -eu

mkdir -p backups

api_id="$(docker compose ps -q api)"
if [ -z "$api_id" ]; then
  echo "Swami Bags API container is not running."
  echo "Start the stack first with: docker compose up -d"
  exit 1
fi

volume_name="$(docker inspect "$api_id" --format '{{range .Mounts}}{{if eq .Destination "/data"}}{{.Name}}{{end}}{{end}}')"
if [ -z "$volume_name" ]; then
  echo "Could not determine the persistent /data volume."
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

docker run --rm \
  -v "$volume_name:/data:ro" \
  -v "$(pwd)/backups:/backup" \
  alpine sh -c "tar czf /backup/$archive -C /data ."

docker compose start api >/dev/null
trap - EXIT INT TERM

echo "Backup written to backups/$archive"
echo "The public Nginx catalogue remains available while the admin service is paused."
