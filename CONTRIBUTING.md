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

Use the existing project convention only. Examples:

    feat/order-service-retry
    fix/kafka-consumer-lag
    chore/bump-quarkus

Never include an agent, model, or tool name in a branch:

    claude/add-endpoint        # wrong
    ai-fix-tests               # wrong
    bot/refactor               # wrong

Feature branches merge into `develop`, and `develop` merges into
`main`.

## Commit messages

- Follow the existing style in `git log`. This project uses
  Conventional Commits.
- Subject line in the imperative mood, 72 characters or less.
- Body explains what changed and why, not that a tool made it.
- No trailers other than ones the project already uses, such as
  `Refs:` or `Closes:`.

## Pull requests

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

    git log --format='%an <%ae>%n%b' origin/main..HEAD
    git rev-parse --abbrev-ref HEAD

Amend or rebase to strip any attribution that slipped in before the
branch is pushed.
