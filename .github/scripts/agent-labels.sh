#!/usr/bin/env bash
#
# Creates (or updates) the labels the issue agent uses.
#
# Safe to run as many times as you like. The hourly workflow runs it before
# every run, so the labels are always there. You can also run it by hand:
#
#   GH_TOKEN=$(gh auth token) .github/scripts/agent-labels.sh
#
set -euo pipefail

REPO="${GITHUB_REPOSITORY:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}"

# name | colour | what it means
LABELS=(
  "agent:ready|0e8a16|Queued for the issue agent to pick up next"
  "agent:working|fbca04|The agent is working on this right now"
  "agent:needs-input|d93f0b|The agent asked a question and is waiting for an answer"
  "agent:blocked|b60205|The agent tried and cannot get any further on its own"
  "agent:done|5319e7|The agent opened a pull request for this"
  "agent:hands-off|ededed|The agent must never pick this up"
  "agent|1d76db|Opened by the issue agent"
)

for entry in "${LABELS[@]}"; do
  IFS='|' read -r name colour description <<< "$entry"
  gh label create "$name" \
    --repo "$REPO" \
    --color "$colour" \
    --description "$description" \
    --force >/dev/null
  echo "label ready: $name"
done
