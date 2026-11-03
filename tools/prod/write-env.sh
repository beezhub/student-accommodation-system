#!/bin/bash
# Runs ON the PROD server, as root (through SSM Run Command, docs/PROD_RUNBOOK.md). Replaces
# /opt/sas with the tree of one commit and writes /opt/sas/.env from SSM Parameter Store and the
# RDS-managed database secret. Prints key names only, never values.
#
# Required environment:
#   SHA             short commit whose tree was uploaded to s3://<backups bucket>/deploy/<SHA>.tar
#   BACKEND_IMAGE   <registry>/student-accommodation-backend@sha256:...  (from a NONPROD deploy)
#   FRONTEND_IMAGE  <registry>/student-accommodation-frontend@sha256:...
set -euo pipefail
export AWS_DEFAULT_REGION=af-south-1 HOME=/root

: "${SHA:?SHA must be set}" "${BACKEND_IMAGE:?BACKEND_IMAGE must be set}" "${FRONTEND_IMAGE:?FRONTEND_IMAGE must be set}"
BACKUPS_BUCKET=beezhub-sas-prod-backups
PARAM_PATH=/sas-prod/
# The RDS-managed master secret (infra/terraform/prod, output database_password_secret_arn). The
# instance role may read this one secret and cannot list RDS, so the ARN is written here.
DB_SECRET_ARN='arn:aws:secretsmanager:af-south-1:428847003911:secret:rds!db-9737be0a-306b-43da-a3fe-57199e95d02f-KP2kEE'

# The tree goes to a staging directory first. Nothing in /opt/sas changes until the new .env is
# known to have every key this commit's compose files need (the preflight below).
stage=/opt/sas.incoming
rm -rf "$stage" && mkdir -p "$stage"
aws s3 cp "s3://$BACKUPS_BUCKET/deploy/$SHA.tar" /tmp/sas.tar --only-show-errors
tar -x -C "$stage" -f /tmp/sas.tar
rm -f /tmp/sas.tar
[ -f "$stage/docker-compose.yml" ] || { echo "the tree for $SHA has no docker-compose.yml; nothing changed" >&2; exit 1; }

# Every value single-quoted, so compose never interpolates a $ inside one.
umask 077
out=/opt/sas/.env.new
: > "$out"
quote() {
  case "$2" in *"'"*) echo "value of $1 contains a single quote; cannot write it safely" >&2; exit 1;; esac
  printf "%s='%s'\n" "$1" "$2" >> "$out"
}

token=""
while :; do
  page=$(aws ssm get-parameters-by-path --path "$PARAM_PATH" --with-decryption --output json \
           ${token:+--starting-token "$token"} --max-items 10)
  printf '%s' "$page" | python3 -c '
import json, sys
for p in json.load(sys.stdin)["Parameters"]:
    print(p["Name"].split("/")[-1] + "\t" + p["Value"])
' > /tmp/params.tsv
  while IFS=$'\t' read -r name value; do quote "$name" "$value"; done < /tmp/params.tsv
  token=$(printf '%s' "$page" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("NextToken",""))')
  [ -n "$token" ] || break
done
rm -f /tmp/params.tsv

# Read on every deploy rather than copied into SSM, so it always matches what RDS holds.
creds=$(aws secretsmanager get-secret-value --secret-id "$DB_SECRET_ARN" --query SecretString --output text)
quote USER_DB "$(printf '%s' "$creds" | python3 -c 'import json,sys; print(json.load(sys.stdin)["username"])')"
quote PASSWORD_DB "$(printf '%s' "$creds" | python3 -c 'import json,sys; print(json.load(sys.stdin)["password"])')"
unset creds
quote BACKEND_IMAGE "$BACKEND_IMAGE"
quote FRONTEND_IMAGE "$FRONTEND_IMAGE"

# Preflight (GH-274): compose aborts partway through `up -d` on a key the files require and .env
# lacks, leaving the stack half recreated. Required means no fallback: ${VAR} or ${VAR:?...}, plus
# every variable a compose secret is read from. The same check deploy.yml runs on NONPROD.
F="docker-compose.yml docker-compose.prod.yml docker-compose.deploy.yml docker-compose.aws.yml"
required=$(cd "$stage" && {
  grep -hoE '\$\{[A-Z0-9_]+(\}|:?\?)' $F | sed -E 's/^\$\{([A-Z0-9_]+).*/\1/' || true
  grep -hoE '^[[:space:]]+environment:[[:space:]]+[A-Z0-9_]+[[:space:]]*$' $F | awk '{print $2}' || true
} | sort -u)
missing=$(comm -23 <(printf '%s\n' "$required") <(cut -d= -f1 "$out" | sort -u) | tr '\n' ' ')
if [ -n "${missing// /}" ]; then
  rm -rf "$out" "$stage"
  echo "the compose files of $SHA need keys that /sas-prod/ does not have: $missing" >&2
  echo "Add each one to Parameter Store (docs/PROD_RUNBOOK.md, 'Changing a setting') and deploy again." >&2
  echo "Nothing on the server was changed." >&2
  exit 1
fi
echo "preflight: .env has all $(printf '%s\n' "$required" | wc -l | tr -d ' ') required keys"

# Now replace the tree: /opt/sas holds only this commit plus .env.
find /opt/sas -mindepth 1 -maxdepth 1 ! -name '.env' ! -name '.env.new' -exec rm -rf {} +
cp -a "$stage/." /opt/sas/
rm -rf "$stage"
chown -R ubuntu:ubuntu /opt/sas
echo "tree: $(find /opt/sas -type f ! -name '.env*' | wc -l) files from $SHA"

chown ubuntu:ubuntu "$out"
chmod 600 "$out"
mv "$out" /opt/sas/.env
echo ".env: $(wc -l < /opt/sas/.env) keys, mode $(stat -c %a /opt/sas/.env)"
cut -d= -f1 /opt/sas/.env | sort | tr '\n' ' '; echo

cd /opt/sas
sudo -u ubuntu docker compose -f docker-compose.yml -f docker-compose.prod.yml -f docker-compose.deploy.yml \
  -f docker-compose.aws.yml config -q && echo "compose config: OK with the four PROD files"
