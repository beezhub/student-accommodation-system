# Setting Scout up

## 1. The labels Scout uses

Scout has no memory between runs. Labels are how it knows what happened last time,
and how you tell it what to do. Create these once:

| Label | Colour | Who sets it | Meaning |
|---|---|---|---|
| `scout:ready` | `#0e8a16` | You | Scout should prefer this issue over anything else. |
| `scout:hands-off` | `#b60205` | You | Scout must never touch this issue. |
| `scout:working` | `#fbca04` | Scout | Scout is on it right now. Do not start it yourself. |
| `scout:needs-input` | `#d93f0b` | Scout | Scout asked a question and will carry on once you answer. |
| `scout:needs-human` | `#5319e7` | Scout | Too big or not Scout's call. A person has to take this one on. |
| `scout:in-review` | `#1d76db` | Scout | Scout has opened a pull request for this. |

Create them from the GitHub issue label screen, or:

```bash
gh label create scout:ready       --color 0e8a16 --description "Scout should pick this up next"
gh label create scout:hands-off   --color b60205 --description "Scout must not touch this"
gh label create scout:working     --color fbca04 --description "Scout is working on this now"
gh label create scout:needs-input --color d93f0b --description "Scout is waiting on a human answer"
gh label create scout:in-review   --color 1d76db --description "Scout has opened a PR for this"
```

Scout still works without them. It just cannot claim issues safely, so two runs could
collide on the same one. Create them.

## 2. Running Scout once, by hand

In Claude Code:

```
/scout:run
```

That does exactly one pass: pick one issue, fix it, open a pull request. Start here.
Watch two or three runs before you leave it unattended.

To see what it is up to without changing anything:

```
/scout:status
```

## 3. Running Scout every hour

Pick one of these. Do not run two at once, they will fight over the same issues.

Scout does not run on GitHub Actions. A run takes up to 45 minutes and the repository's
Actions minutes are metered and close to the monthly allowance (GH-284), so an hourly
workflow would spend the whole budget in days. The Routine below costs no minutes.

### Option A: a scheduled session (simplest)

In Claude Code on the web, ask for a Routine that runs `/scout:run` on the hour.
Each run is a fresh session, which is exactly what Scout is built for.

### Option B: your own machine

```
/loop 1h /scout:run
```

Runs while that Claude Code session is open. Good for a day of supervised running.
Closing the session stops it.

## 4. Turning Scout off

- **One issue:** put `scout:hands-off` on it.
- **Everything, right now:** stop the Routine or the loop.
- **Permanently:** delete `.claude/agents/scout.md`.

Scout never merges anything. Every change it makes waits for a person to approve it,
so the worst an unattended Scout can do is open pull requests nobody wants.

## 5. What to check in the first week

- Are the pull requests small? Scout is meant to make small ones. Large PRs mean the
  issue filter in `playbook.md` Step 3 is letting through work that is too big.
- Are the questions answerable by someone who is not a developer? If not, tighten
  `asking-for-help.md`. That is the file that decides how it talks.
- Is it picking sensible issues? Use `scout:ready` to steer it, and `scout:hands-off`
  to fence off what you want to keep.
