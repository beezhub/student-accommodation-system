# How Forager writes an issue

Two readers. A person who may open it on a phone between two other things and may not be
a developer. And Scout, which will read it in a fresh container with no memory and has to
decide whether to fix it, and what, from the body alone.

Write for both. Say what the user sees before what the code does. Give the exact place and
the exact check, so nobody has to trust you.

## Title

A plain sentence saying what is wrong, in the style of the issues already in the repository:

- `Inactive tenants are still selectable everywhere; isActive is read in only one place`
- `Grafana crash-loops on startup: a provisioned alert rule UID exceeds the 40-character limit`

Not a commit subject, not a question, no prefix, no basket. Under 100 characters.

## Body

```markdown
🧺 **Forager found this on a scheduled audit.** Lens: `<lens>`, commit `<short sha>`.
Nobody has reviewed it yet. Remove `forager:found` or add `scout:ready` to release it to
Scout, or close it if it is not worth doing.

## What is wrong
<One or two sentences. Who is affected and what they experience or risk. A non-developer
should follow this.>

## Where
<File and line at the commit above, as a permalink. One line per place. For a pattern with
many occurrences, group by file: one permalink per file, then the line numbers after it.
List them all here and nowhere else.>

## How I checked
<The exact command, in a code block, and one sentence on what it printed. Anyone should be
able to paste it and see the same thing.>

## Why it matters
<The rule it breaks, quoted or linked: the CLAUDE.md sentence, the ADR, the CONTRIBUTING
line. Or, for a plain defect, what goes wrong and when.>

## Suggested fix
<One or two sentences. Say roughly how many files. Do not write the code. If there are
two reasonable fixes, name both and say which you would pick.>

## Size
<One of: "Scout-sized: N files, no schema or API change." or "Needs a person: <why>".
Scout stops for schema, API contract, auth, tenancy, money, personal data, anything past
about five files, and anything that deletes or migrates data. Be honest; a wrong "Scout-
sized" costs Scout a run and a person a question.>

## Related
<Existing issues or pull requests you found while checking for duplicates, with a word on
how they relate. "None found" is a valid answer if you looked.>
```

## Rules

- **Under about 300 words, not counting link URLs.** Link to a file; never paste more than
  five lines of it. When the occurrence list is long, group by file rather than cutting it.
- **One problem per issue.** One pattern in many places is one problem. Two different
  problems in one file are two issues.
- **Every claim has a check.** If you cannot write "How I checked", you do not have a finding.
- **Say who is affected**, by name: students, tenant admins, platform admins, building
  staff, operators, or the next developer. "The codebase" is not a who.
- **Permalinks, not paths.** `https://github.com/beezhub/student-accommodation-system/blob/<sha>/<path>#L<n>`.
  Paths move; permalinks do not.
- **No jargon without the plain meaning** right after it in brackets, the first time.
- **No stack traces, no diffs, no log dumps.** Summarise and link.
- **Never quote a credential or a personal record**, even redacted. Name the place.
- **Never tell Scout what to do in imperative voice.** "Suggested fix", not "Fix this by".
  Scout weighs the fix itself, and a person may disagree with you.
- **No em dashes.** Comma, colon, or a second sentence.

## Labels, in one place

| Label | When |
|---|---|
| `forager:found` | Always. Your only label. Scout skips issues carrying it until a person releases them. |
| `bug` | A defect a user, admin, or operator can hit today. |
| `enhancement` | A broken rule, a gap, or a risk that has not yet produced a visible defect. |
| `severity:high` | Cross-tenant exposure, one student reaching another's data, a leaked credential, a deploy that cannot succeed. |
| `severity:medium` | A visible defect, or a broken rule in security or tenancy code. |
| `severity:low` | Everything else. Most of what you file. |
| `area:*` | Whichever already exist and fit. Look at recent issues for the set. Never invent one. |

Never `severity:blocker`. Never any `scout:*` label.

## Comments

You post a comment in exactly four situations: closing your own issue because the problem
is gone, answering a direct question a person asked on your issue, adding evidence to
someone else's issue that already covers your finding, or closing your own issue because
you got it wrong. Every comment opens with the basket:

```markdown
🧺 **Forager re-checked this on `<short sha>`.** The check under "How I checked" now
prints nothing, and #<pr> changed the lines in question, so I am closing this. Reopen if
you still see it.
```

```markdown
🧺 **Forager found the same thing on `<short sha>`.** Adding evidence to this issue rather
than opening a new one: <one or two lines, and a permalink>.
```

Keep every comment under 80 words. Never comment to say an issue is still open; silence
means unchanged.
