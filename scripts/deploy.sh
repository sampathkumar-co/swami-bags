#!/usr/bin/env sh
set -eu

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
repo_dir="$(dirname "$script_dir")"
cd "$repo_dir"

sh scripts/preflight.sh

echo "Building Swami Bags production containers..."
docker compose build

echo "Starting production stack..."
docker compose up -d

echo "Waiting for public health endpoint..."
attempt=1
while [ "$attempt" -le 40 ]; do
  if curl -fsS "http://127.0.0.1:${WEB_PORT:-8088}/healthz" >/dev/null 2>&1; then
    echo "Swami Bags is healthy."
    docker compose ps
    exit 0
  fi
  attempt=$((attempt + 1))
  sleep 2
done

echo "Deployment started but health verification failed."
docker compose ps
docker compose logs --tail=120
exit 1
