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

# The tree, fresh: /opt/sas holds only this commit plus .env.
aws s3 cp "s3://$BACKUPS_BUCKET/deploy/$SHA.tar" /tmp/sas.tar --only-show-errors
find /opt/sas -mindepth 1 -maxdepth 1 ! -name '.env' -exec rm -rf {} +
tar -x -C /opt/sas -f /tmp/sas.tar
rm -f /tmp/sas.tar
chown -R ubuntu:ubuntu /opt/sas
echo "tree: $(find /opt/sas -type f | wc -l) files from $SHA"

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

chown ubuntu:ubuntu "$out"
chmod 600 "$out"
mv "$out" /opt/sas/.env
echo ".env: $(wc -l < /opt/sas/.env) keys, mode $(stat -c %a /opt/sas/.env)"
cut -d= -f1 /opt/sas/.env | sort | tr '\n' ' '; echo

cd /opt/sas
sudo -u ubuntu docker compose -f docker-compose.yml -f docker-compose.prod.yml -f docker-compose.deploy.yml \
  -f docker-compose.aws.yml config -q && echo "compose config: OK with the four PROD files"
