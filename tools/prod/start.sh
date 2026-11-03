#!/bin/bash
# Runs ON the PROD server, as root, after write-env.sh (docs/PROD_RUNBOOK.md). Logs in to ECR with
# the instance role, pulls the images .env names, starts the stack and waits for readiness.
set -uo pipefail
export AWS_DEFAULT_REGION=af-south-1 HOME=/root
REGISTRY=428847003911.dkr.ecr.af-south-1.amazonaws.com
cd /opt/sas || exit 1
F="-f docker-compose.yml -f docker-compose.prod.yml -f docker-compose.deploy.yml -f docker-compose.aws.yml"

aws ecr get-login-password | sudo -u ubuntu docker login --username AWS --password-stdin "$REGISTRY" 2>&1 | tail -1
sudo -u ubuntu docker compose $F pull --quiet student-accommodation-back-end frontend redis alloy && echo "pull: OK"
sudo -u ubuntu docker compose $F up -d --no-build --remove-orphans 2>&1 | tail -6

# Generous: on a first boot, or after a large migration, Flyway runs in-process before readiness.
NET=$(docker inspect student-accommodation-back-end --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}')
ready=no
for i in $(seq 1 120); do
  if docker run --rm --network "$NET" curlimages/curl:8.11.1 -fsS -m 5 \
       http://student-accommodation-back-end:8080/actuator/health/readiness >/dev/null 2>&1; then
    echo "readiness OK after $((i*5))s"
    ready=yes
    break
  fi
  sleep 5
done

echo "== running images:"
for c in student-accommodation-back-end student-accommodation-frontend; do
  docker inspect "$c" --format '{{.Name}} {{.Config.Image}} health={{.State.Health.Status}}' \
    | sed -E "s#$REGISTRY/##; s#(sha256:[a-f0-9]{12})[a-f0-9]+#\1#"
done
echo "errors since start: $(docker logs student-accommodation-back-end 2>&1 | grep -c '"level":"ERROR"')"
free -m | awk 'NR==2{print "memory used "$3" of "$2" MB"} NR==3{print "swap used "$3" MB"}'

if [ "$ready" != yes ]; then
  echo "READINESS NEVER CAME UP"
  sudo -u ubuntu docker compose $F ps
  docker logs --tail=200 student-accommodation-back-end
  exit 1
fi
