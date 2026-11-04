# Contributing

These rules apply to every contribution, whether it is written by a
person or produced with tooling assistance.

## Attribution

Do not identify a contribution as agent-generated anywhere in the
repository or its history. Specifically:

- No `Co-Authored-By:` trailers naming an AI, model, or tool.
- No "Generated with", "Created by", or similar footers in commit
  messages, PR titles, or PR descriptions.
- No robot emoji, sparkle emoji, or other AI markers.
- Do not add attribution to code comments, changelogs, or docs.
- Do not mention that a change was AI-assisted in review comments.

Commits are authored under the repository user's configured
`user.name` and `user.email`. Never override these with `--author`,
`GIT_AUTHOR_*`, or `GIT_COMMITTER_*`.

## Branch names

Use the existing project convention only. Branch from `develop` and
name the branch `feature/TASK-{id}-{slug}`:

    feature/TASK-243-deploy-nonprod-on-develop
    fix/migration-lock
    hotfix/fix-composite-indexes-migration

Never include an agent, model, or tool name in a branch:

    claude/add-endpoint        # wrong
    ai-fix-tests               # wrong
    bot/refactor               # wrong

Always branch from `develop`, never from `main`, except for a hotfix
(below). Feature branches merge back into `develop` via pull request, and
`develop` merges into `main`. Never commit directly to `main` or
`develop`.

### Hotfixes

A hotfix is the one exception: a fix for a PROD bug that cannot wait for
the next release. Everything else goes through `develop`.

- Branch from `main` and name it `hotfix/TASK-{id}-{slug}`.
- Open the pull request against `main`. If `main` changed after you
  branched, merge `main` into your branch first, so CI tests exactly what
  will land.
- Merge with a merge commit, never a squash.
- It deploys like a release: NONPROD first, then Deploy PROD.
- A workflow then opens a pull request from `main` into `develop`. Merge
  it, so the next release keeps the fix.

The full flow, with a diagram, is in `docs/BRANCHING_STRATEGY.md`.

## Commit messages

- Follow the existing style in `git log`. This project uses
  Conventional Commits.
- Subject line in the imperative mood, 72 characters or less.
- Body explains what changed and why, not that a tool made it.
- No trailers other than ones the project already uses, such as
  `Refs:` or `Closes:`.

## Pull requests

- Open the pull request against `develop`. A hotfix opens it against
  `main` (see Hotfixes).
- Merge with a merge commit, never a squash. A squash makes `main` and
  `develop` look unrelated to Git, and later releases conflict.
- A pull request may close several issues when they are related: the
  same area, the same kind of change (such as one bug on several
  screens), and reviewable and revertible as one piece. Then:
  - one commit per issue, so each fix stays readable on its own;
  - a `Closes #n` line for each issue in the pull request body;
  - the branch named after the first issue, `feature/TASK-{id}-{slug}`;
  - a title that says what the issues have in common.
- Do not bundle unrelated issues, and do not fold drive-by fixes or
  refactors into a pull request that is about something else. Open a
  second branch and a second pull request instead.
- Title and description describe the change only.
- No tooling disclosure section, no "how this was made" notes.
- Fill in the repository PR template as a human contributor would.

## Writing style

- Do not use em dashes. Use a comma, a colon, or a separate sentence.
- Do not use en dashes as sentence punctuation. Numeric ranges such as
  `1-10` are fine with a hyphen.
- Plain, direct sentences. No filler adjectives.

## Verification before pushing

Run these and confirm the output is clean:

    git log --format='%an <%ae>%n%b' origin/develop..HEAD
    git rev-parse --abbrev-ref HEAD

For a hotfix, compare against the branch it came from:
`origin/main..HEAD`.

Amend or rebase to strip any attribution that slipped in before the
branch is pushed.
