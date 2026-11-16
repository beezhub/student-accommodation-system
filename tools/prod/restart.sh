#!/bin/bash
# Runs ON the PROD server, as root, through tools/prod/ssmrun.sh (restart-prod.yml, or by hand from
# docs/PROD_RUNBOOK.md). Restarts running containers in place and waits for the backend to be ready.
#
#   VARS='TARGET=backend' tools/prod/ssmrun.sh tools/prod/restart.sh
#
# TARGET is one of: backend, frontend, redis, alloy, all.
#
# `docker restart` keeps each container's image, .env values and mounts exactly as the last deploy
# left them. It does not pull, change a version or run anything new, so it never needs a rollback.
# To pick up a changed setting or secret, redeploy instead: write-env.sh rewrites .env and start.sh
# recreates what changed.
set -uo pipefail
REGISTRY=428847003911.dkr.ecr.af-south-1.amazonaws.com

case "${TARGET:-}" in
  backend)  containers="student-accommodation-back-end" ;;
  frontend) containers="student-accommodation-frontend" ;;
  redis)    containers="student-accommodation-redis" ;;
  alloy)    containers="student-accommodation-alloy" ;;
  # Redis first, so the backend comes back to a Redis that is already up.
  all)      containers="student-accommodation-redis student-accommodation-back-end student-accommodation-frontend student-accommodation-alloy" ;;
  *)        echo "TARGET must be backend, frontend, redis, alloy or all, not '${TARGET:-}'"; exit 1 ;;
esac

for c in $containers; do
  docker inspect "$c" >/dev/null 2>&1 || { echo "$c does not exist on this server; nothing was restarted"; exit 1; }
done

for c in $containers; do
  echo "restarting $c"
  docker restart --time 30 "$c" >/dev/null || { echo "RESTART FAILED for $c"; exit 1; }
done

# The backend is the slow one: readiness waits for Spring, the database pool and Redis. Checked even
# when only Redis restarted, because the backend reads Redis on every request it rate-limits.
NET=$(docker inspect student-accommodation-back-end --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}')
ready=no
for i in $(seq 1 60); do
  if docker run --rm --network "$NET" curlimages/curl:8.11.1 -fsS -m 5 \
       http://student-accommodation-back-end:8080/actuator/health/readiness >/dev/null 2>&1; then
    echo "readiness OK after $((i*5))s"
    ready=yes
    break
  fi
  sleep 5
done

echo "== containers:"
docker ps --format '{{.Names}} {{.Status}}' | grep '^student-accommodation-' | sort
for c in student-accommodation-back-end student-accommodation-frontend; do
  docker inspect "$c" --format '{{.Name}} {{.Config.Image}}' \
    | sed -E "s#$REGISTRY/##; s#(sha256:[a-f0-9]{12})[a-f0-9]+#\1#"
done
echo "errors since start: $(docker logs student-accommodation-back-end 2>&1 | grep -c '"level":"ERROR"')"
free -m | awk 'NR==2{print "memory used "$3" of "$2" MB"} NR==3{print "swap used "$3" MB"}'

if [ "$ready" != yes ]; then
  echo "READINESS NEVER CAME UP"
  docker logs --tail=200 student-accommodation-back-end
  exit 1
fi
