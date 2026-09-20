#!/usr/bin/env sh
set -eu

archive="${1:-}"
[ -n "$archive" ] || {
  echo "Usage: sh scripts/restore.sh backups/swami-data-YYYYMMDD-HHMMSS.tgz"
  exit 1
}
[ -f "$archive" ] || {
  echo "Backup archive not found: $archive"
  exit 1
}

api_id="$(docker compose ps -q api)"
if [ -z "$api_id" ]; then
  echo "Start the stack once before restoring so the persistent volume exists."
  exit 1
fi

volume_name="$(docker inspect "$api_id" --format '{{range .Mounts}}{{if eq .Destination "/data"}}{{.Name}}{{end}}{{end}}')"
[ -n "$volume_name" ] || {
  echo "Could not determine the persistent /data volume."
  exit 1
}

echo "This will replace the current Swami Bags database, media, and generated catalogue."
printf "Type RESTORE to continue: "
read -r confirmation
[ "$confirmation" = "RESTORE" ] || {
  echo "Restore cancelled."
  exit 1
}

archive_dir="$(CDPATH= cd -- "$(dirname -- "$archive")" && pwd)"
archive_name="$(basename -- "$archive")"

echo "Stopping the stack for a consistent restore..."
docker compose stop

docker run --rm \
  -v "$volume_name:/data" \
  -v "$archive_dir:/backup:ro" \
  alpine sh -c "rm -rf /data/* /data/.[!.]* /data/..?* 2>/dev/null || true; tar xzf /backup/$archive_name -C /data"

echo "Starting restored stack..."
docker compose start
echo "Restore completed. Verify the website and admin before deleting any backup."
