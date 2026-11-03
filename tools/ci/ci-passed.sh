#!/bin/bash
# Runs in deploy.yml before anything is built (#385). Exits 0 only if a successful CI run tested
# the same files as the commit being deployed, and waits for one that is still running.
#
#   tools/ci/ci-passed.sh <sha>
#
# Compares files, not commits. A release's merge commit on main has the same files as the develop
# commit CI tested; a hotfix has the same files its pull request tested; a docs-only merge differs
# from the last tested commit only in paths CI ignores. One rule covers all three, and none of them
# needs a CI run on main, which no longer has one.
#
# Needs: git history (checkout with fetch-depth: 0), gh with GH_TOKEN, GITHUB_REPOSITORY.
# CI_WAIT_SECONDS (default 1200) bounds the wait for a run in progress.
set -euo pipefail
target=${1:?usage: ci-passed.sh <sha>}
repo=${GITHUB_REPOSITORY:?GITHUB_REPOSITORY must be set}
wait_seconds=${CI_WAIT_SECONDS:-1200}
ci=.github/workflows/ci.yml

# The paths ci.yml's push trigger ignores, read from ci.yml itself so the two never drift. GitHub's
# '**/*.md' also matches a root README.md, so a leading '**/' is dropped, and '**' becomes '*',
# which in a bash pattern already crosses '/'. If this finds nothing the check only gets stricter.
mapfile -t ignored < <(awk '
  /^  push:/ { push = 1; next }
  /^  [a-z_]+:/ { push = 0 }
  push && /paths-ignore:/ { list = 1; next }
  list && /^      - / { gsub(/^      - |\x27|"/, ""); print; next }
  list { list = 0 }
' "$ci" | sed -e 's#^\*\*/##' -e 's#\*\*#*#g')

is_ignored() {
  local pattern
  for pattern in "${ignored[@]}"; do
    # shellcheck disable=SC2053 # the right-hand side is a pattern on purpose
    [[ $1 == $pattern ]] && return 0
  done
  return 1
}

# True when <sha> is in this checkout and differs from the target only in ignored paths.
covers() {
  local file
  git cat-file -e "$1^{commit}" 2>/dev/null || return 1
  while IFS= read -r file; do
    is_ignored "$file" || return 1
  done < <(git diff --name-only "$1" "$target")
  return 0
}

# The newest CI runs with the given status, as head SHAs.
runs() {
  gh api "repos/$repo/actions/workflows/ci.yml/runs?status=$1&per_page=100" --jq '.workflow_runs[].head_sha'
}

echo "ignored by CI: ${ignored[*]:-(none found)}"
deadline=$(( $(date +%s) + wait_seconds ))
while :; do
  for sha in $(runs success); do
    if covers "$sha"; then
      echo "CI passed on ${sha:0:7}, which has the same files as ${target:0:7} apart from ignored paths"
      exit 0
    fi
  done

  pending=""
  for status in in_progress queued; do
    for sha in $(runs "$status"); do
      covers "$sha" && pending=$sha
    done
  done
  if [ -z "$pending" ]; then
    echo "::error::no successful CI run tested the files of ${target:0:7}. Check CI on the commit being released (for a release, the develop head; for a hotfix, its pull request) and re-run it if it failed or was cancelled."
    exit 1
  fi
  if [ "$(date +%s)" -ge "$deadline" ]; then
    echo "::error::CI on ${pending:0:7} was still running after ${wait_seconds}s"
    exit 1
  fi
  echo "CI on ${pending:0:7} is still running; checking again in 30s"
  sleep 30
done
