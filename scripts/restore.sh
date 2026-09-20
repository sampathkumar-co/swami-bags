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

repo_dir="$(pwd -P)"
backup_dir="$repo_dir/backups"
archive_dir="$(CDPATH= cd -- "$(dirname -- "$archive")" && pwd -P)"
archive_name="$(basename -- "$archive")"

[ "$archive_dir" = "$backup_dir" ] || {
  echo "For a safe restore, copy the archive into $backup_dir first."
  exit 1
}
case "$archive_name" in
  *[!A-Za-z0-9._-]*|'') echo "Backup filename contains unsupported characters."; exit 1 ;;
esac

api_id="$(docker compose ps -q api)"
if [ -z "$api_id" ]; then
  echo "Start the stack once before restoring so the persistent volume exists."
  exit 1
fi

echo "This will replace the current Swami Bags database, media, and generated catalogue."
printf "Type RESTORE to continue: "
read -r confirmation
[ "$confirmation" = "RESTORE" ] || {
  echo "Restore cancelled."
  exit 1
}

echo "Stopping the stack for a consistent restore..."
docker compose stop

docker compose --profile ops run --rm -T -e "ARCHIVE=$archive_name" ops   sh -c 'rm -rf /data/* /data/.[!.]* /data/..?* 2>/dev/null || true; tar xzf "/backup/$ARCHIVE" -C /data'

echo "Starting restored stack..."
docker compose start
echo "Restore completed. Verify the website and admin before deleting any backup."
