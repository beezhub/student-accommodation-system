#!/bin/bash
# Runs ON the operator's machine. Sends a local bash script to the PROD server through SSM Run
# Command and prints its output (docs/PROD_RUNBOOK.md). There is no SSH: port 22 is closed.
#
#   tools/prod/ssmrun.sh tools/prod/start.sh
#   VARS='SHA=abc1234 BACKEND_IMAGE=... FRONTEND_IMAGE=...' tools/prod/ssmrun.sh tools/prod/write-env.sh
#
# VARS, if set, is exported in front of the script on the server. SSM runs commands with sh (dash on
# Ubuntu), which has no `set -o pipefail`, so the script is written to a file and run with bash.
set -euo pipefail
INSTANCE_ID=${INSTANCE_ID:-i-0275831c4e2972f2a}
REGION=af-south-1
script=${1:?usage: ssmrun.sh <script> (VARS='K=V ...' optional)}

params=$(VARS="${VARS:-}" node -e '
const fs = require("fs");
const body = fs.readFileSync(process.argv[1], "utf8");
const eof = "SSM_SCRIPT_EOF";
if (body.includes(eof)) throw new Error("script contains the heredoc delimiter");
const vars = process.env.VARS ? "export " + process.env.VARS + "\n" : "";
console.log(JSON.stringify({
  commands: [`cat > /tmp/ssm-script.sh <<'"'"'${eof}'"'"'\n${vars}${body}\n${eof}`,
             "bash /tmp/ssm-script.sh; rc=$?; rm -f /tmp/ssm-script.sh; exit $rc"],
  executionTimeout: ["900"],
}));' "$script")

id=$(MSYS_NO_PATHCONV=1 aws ssm send-command --region "$REGION" --instance-ids "$INSTANCE_ID" \
       --document-name AWS-RunShellScript --timeout-seconds 900 --parameters "$params" \
       --query Command.CommandId --output text)
status=Pending
for _ in $(seq 1 300); do
  status=$(aws ssm get-command-invocation --region "$REGION" --command-id "$id" \
             --instance-id "$INSTANCE_ID" --query Status --output text 2>/dev/null || echo Pending)
  case "$status" in Success|Failed|Cancelled|TimedOut) break;; esac
  sleep 3
done
echo "[$status]"
aws ssm get-command-invocation --region "$REGION" --command-id "$id" --instance-id "$INSTANCE_ID" \
  --query '[StandardOutputContent,StandardErrorContent]' --output text
[ "$status" = Success ]
