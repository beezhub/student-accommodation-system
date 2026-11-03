#!/bin/bash
# Runs in back-merge.yml on every push to main (#386). Opens a pull request from main into develop
# when main holds a change develop lacks, which after a hotfix it does. Without it the fix stays
# on main only: #288 sat there for two months and had to be re-applied by hand in #371.
#
# The question is "would merging main into develop change develop?", answered with
# `git merge-tree`, which merges in memory and touches no branch. Comparing the two branches' files
# would misfire: develop is usually ahead of main with features not yet released, so their files
# differ right after a normal release, when main has nothing develop lacks.
#
# Needs: git history (checkout with fetch-depth: 0), gh with GH_TOKEN, and the repository setting
# "Allow GitHub Actions to create and approve pull requests".
set -euo pipefail
# Overridable so the script can be tried on throwaway commits.
develop=$(git rev-parse "${DEVELOP_REF:-origin/develop}")
main=$(git rev-parse "${MAIN_REF:-origin/main}")

if merged=$(git merge-tree --write-tree "$develop" "$main"); then
  if [ "$(head -1 <<< "$merged")" = "$(git rev-parse "$develop^{tree}")" ]; then
    echo "develop already has everything on main; nothing to merge back"
    exit 0
  fi
  conflicts="It merges without conflicts."
else
  conflicts="**It conflicts.** Resolve it on a branch from \`develop\`: merge \`origin/main\` into that branch, fix the conflicts, and open that branch's pull request into \`develop\` instead of merging this one."
fi

open=$(gh pr list --base develop --head main --state open --json number --jq '.[0].number // empty')
if [ -n "$open" ]; then
  echo "back-merge pull request #$open is already open; it picks up this push on its own"
  exit 0
fi

commits=$(git log --no-merges --format='- %h %s' "$develop..$main")
body=$(cat <<EOF
\`main\` has changes \`develop\` lacks, usually from a hotfix (\`docs/BRANCHING_STRATEGY.md\`, "A hotfix"). Merge this so the next release keeps them.

Commits on \`main\` that \`develop\` does not have:

${commits:-"- (merge commits only)"}

$conflicts

- Merge with a **merge commit**, not a squash, or the two branches drift apart again (#104).
- CI does not start on this pull request, because it was opened by a workflow. The changes passed CI on their own pull request, and merging into \`develop\` runs CI as a normal push.

Opened by \`.github/workflows/back-merge.yml\` (#386).
EOF
)
gh pr create --base develop --head main --title "chore(release): merge main back into develop" --body "$body"
