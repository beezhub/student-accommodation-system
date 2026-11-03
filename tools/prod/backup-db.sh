#!/bin/bash
# Daily logical backup of PROD's database, kept outside RDS (#339). Installed on the PROD server as
# /usr/local/sbin/sas-backup-db and run by the sas-backup-db.timer systemd unit (install-backup.sh).
#
# RDS's own automated backups give point-in-time recovery for 7 days, but they live with the
# instance: deleting the instance by mistake, or losing the account, takes them too. This dump goes
# to the separate backups bucket, where the lifecycle rule keeps 35 days of them.
#
# Success is reported as the CloudWatch metric SAS/Backups BackupSucceeded. The sas-prod-db-backup-
# missing alarm fires when none arrives for two days, so a run that silently stops is noticed.
set -euo pipefail
export AWS_DEFAULT_REGION=af-south-1 HOME=/root

DB_HOST=sas-prod.c7q02yequ7f3.af-south-1.rds.amazonaws.com
DB_NAME=studentaccommodation
BUCKET=beezhub-sas-prod-backups
DB_SECRET_ARN='arn:aws:secretsmanager:af-south-1:428847003911:secret:rds!db-9737be0a-306b-43da-a3fe-57199e95d02f-KP2kEE'

stamp=$(date -u +%Y%m%dT%H%MZ)
dump=$(mktemp /var/tmp/sas-prod-XXXXXX.dump)
trap 'rm -f "$dump"' EXIT

# The password is read from the RDS-managed secret on every run, never stored on disk.
creds=$(aws secretsmanager get-secret-value --secret-id "$DB_SECRET_ARN" --query SecretString --output text)
PGUSER=$(printf '%s' "$creds" | python3 -c 'import json,sys; print(json.load(sys.stdin)["username"])')
PGPASSWORD=$(printf '%s' "$creds" | python3 -c 'import json,sys; print(json.load(sys.stdin)["password"])')
export PGUSER PGPASSWORD
unset creds

# Custom format: compressed, and pg_restore can restore one table at a time.
pg_dump "host=$DB_HOST dbname=$DB_NAME sslmode=require" -Fc --no-owner --no-privileges -f "$dump"
unset PGPASSWORD

# A dump pg_restore cannot list is not a backup.
objects=$(pg_restore --list "$dump" | grep -cv '^;')
[ "$objects" -gt 0 ] || { echo "dump lists no objects; not uploading" >&2; exit 1; }
bytes=$(stat -c %s "$dump")

key="postgres/sas-prod-$stamp.dump"
aws s3 cp "$dump" "s3://$BUCKET/$key" --only-show-errors

aws cloudwatch put-metric-data --namespace SAS/Backups --metric-name BackupSucceeded \
  --dimensions Database=sas-prod --value 1 --unit Count

echo "action=dbBackup result=success key=$key bytes=$bytes objects=$objects"
