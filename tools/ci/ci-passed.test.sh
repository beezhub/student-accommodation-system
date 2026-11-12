#!/bin/bash
# Tests for ci-passed.sh. Run from the repository root: bash tools/ci/ci-passed.test.sh
#
# A fake `gh` on PATH stands in for the GitHub API. It fails its first FAKE_GH_FAILURES calls, the
# way a 502 does, then reports HEAD as the head of a successful CI run, so HEAD is always covered.
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
cat > "$work/gh" <<'FAKE'
#!/bin/bash
count=$(( $(cat "$FAKE_GH_COUNT" 2>/dev/null || echo 0) + 1 ))
echo "$count" > "$FAKE_GH_COUNT"
if [ "$count" -le "$FAKE_GH_FAILURES" ]; then
  echo "gh: Server Error (HTTP 502)" >&2
  exit 1
fi
case "$*" in
  *status=success*) git rev-parse HEAD ;;
esac
FAKE
chmod +x "$work/gh"

failed=0
# run_gate <failures before gh answers> -> sets $status and $output
run_gate() {
  rm -f "$work/count"
  output=$(PATH="$work:$PATH" FAKE_GH_COUNT="$work/count" FAKE_GH_FAILURES=$1 \
    GITHUB_REPOSITORY=beezhub/test CI_RETRY_DELAY=0 \
    bash tools/ci/ci-passed.sh "$(git rev-parse HEAD)" 2>&1)
  status=$?
}
check() {
  if eval "$2"; then echo "ok   $1"; else echo "FAIL $1"; echo "$output" | sed 's/^/     /'; failed=1; fi
}

run_gate 0
check "apiAnswers_ciPassedOnTheSameFiles_passes" '[ $status -eq 0 ]'

run_gate 2
check "apiFailsTwiceThenAnswers_retriesAndPasses" '[ $status -eq 0 ]'

run_gate 99
check "apiKeepsFailing_fails" '[ $status -ne 0 ]'
check "apiKeepsFailing_saysItIsTheApiNotCi" '[[ $output == *"GitHub API"* ]]'
check "apiKeepsFailing_doesNotBlameCi" '[[ $output != *"no successful CI run"* ]]'

exit $failed
