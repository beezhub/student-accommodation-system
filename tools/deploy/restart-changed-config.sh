#!/bin/bash
# Runs ON a server, after `docker compose up -d`, in both deploys (#411). Restarts any running
# container whose mounted config file no longer matches the release.
#
# A deploy replaces the files under /opt/sas. A container mounting one keeps the old file until it
# restarts, and Compose only recreates a container when its settings change, not when a mounted
# file's content does. That left NONPROD's Alloy running without the token masking for days.
#
# The mounts come from docker inspect, so a new config file needs no change here. A container whose
# files all match is left alone, so an ordinary deploy does not restart the database. Directories are
# skipped. Prints file names only, never their content.
set -uo pipefail
ROOT=${1:-/opt/sas}
failed=no
restarted=0

for container in $(docker ps --format '{{.Names}}'); do
  changed=""
  while read -r source target; do
    case "$source" in "$ROOT"/*) ;; *) continue ;; esac
    [ -f "$source" ] || continue
    # docker exec prints its own failures to stdout, so only a real checksum counts as an answer.
    # An image with no sha256sum would otherwise look changed, and restart on every deploy.
    inside=$(docker exec "$container" sha256sum "$target" 2>/dev/null </dev/null | cut -d' ' -f1)
    if ! [[ "$inside" =~ ^[0-9a-f]{64}$ ]]; then
      echo "warning: cannot read $target in $container, so it was not compared"
      continue
    fi
    [ "$(sha256sum "$source" | cut -d' ' -f1)" = "$inside" ] || changed="$changed $target"
  done < <(docker inspect -f '{{range .Mounts}}{{if eq .Type "bind"}}{{.Source}} {{.Destination}}{{"\n"}}{{end}}{{end}}' "$container")

  if [ -n "$changed" ]; then
    echo "restarting $container, changed:$changed"
    if docker restart "$container" >/dev/null; then
      restarted=$((restarted + 1))
    else
      echo "error: could not restart $container"
      failed=yes
    fi
  fi
done

echo "config check: $restarted container(s) restarted"
[ "$failed" = no ]
