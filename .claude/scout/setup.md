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

## 3. Running Scout unattended

Pick one of these. Do not run two at once, they will fight over the same issues. The
cadence is in section 3b.

Scout does not run on GitHub Actions. A run takes up to 45 minutes and the repository's
Actions minutes are metered and close to the monthly allowance (GH-284), so a scheduled
workflow would spend the whole budget in days. The Routine below costs no minutes.

### Option A: a Routine (the way to run it unattended)

A Routine only has a repository if you pick one when you create it. A Routine created
without one, for example from inside a session, starts each run with no code checked out,
so Scout wakes up in an empty container and can do nothing. Create it from the web form:

1. Go to `claude.ai/code/routines` and click **New routine**.
2. Name it `Scout`. Prompt: `/scout:run`. The command is committed in this repository, so
   the run finds it as soon as the clone finishes.
3. Under **Select repositories**, add `beezhub/student-accommodation-system`. This is the
   step that is easy to miss and the one that matters. Every run clones it fresh from
   `main`; the playbook fetches `develop` itself.
4. Pick your environment. Trusted network access is enough: GitHub and the package
   registries are on the default allowlist.
5. Trigger: **Schedule**, weekdays. The form's presets allow one time a day; for the three
   daily times in section 3b, set the cron `0 7,11,15 * * 1-5` with `/schedule update` from
   a terminal, or create three Routines.
6. Connectors: remove everything except GitHub. Scout needs nothing else.
7. Create, then **Run now** and open the run. A green run means the session finished, not
   that Scout did anything useful; read the report at the end of the transcript.

Commits from a Routine carry your GitHub user, which is what the contribution rules ask
for. Routines count against a daily run allowance; the number is on the routines page.

### Option A, from a phone

The routines web form is not available in the mobile app. The same result can be built
from any Claude Code session that has this repository open, by asking it to:

1. Create a new cloud session with this repository as its source, checked out at
   `develop`, titled `Scout: scheduled runs`. That session is Scout's home: it keeps the
   repository and the GitHub tools between runs.
2. Create a schedule that fires into that session, on the cadence in section 3b, with a
   prompt that first runs `git fetch origin develop && git checkout -B develop
   origin/develop` and then `/scout:run`.

Each firing arrives as a new message in the home session, which shows up in the mobile
app's session list like any other session, so runs can be read on the phone. The
conversation in that session grows with every run; it is compacted automatically, and
Scout rebuilds its state from GitHub anyway, so that is harmless. Once a month or so,
archive it and create a fresh one to keep it quick.

### 3b. Cadence

Do not run Scout hourly once the backlog is short. Most runs would boot, find nothing, and
count against the daily run allowance. Three runs on weekdays, placed after the times a
person acts, is enough for a supply of a few issues a day:

| When (SAST) | Why then |
|---|---|
| 09:00 | Picks up what was released or answered in the morning. |
| 13:00 | Second issue of the day, plus any CI or review fixes from the morning. |
| 17:00 | Handles review comments left during the day, so the pull request is mergeable by evening. |

Cron, in UTC: `0 7,11,15 * * 1-5`. Nothing at weekends: nobody reviews then, and Scout
stops at five open pull requests anyway. For a burst, run `/scout:run` by hand.

### Option B: your own machine

```
/loop 1h /scout:run
```

Runs while that Claude Code session is open. Good for a day of supervised running.
Closing the session stops it.

## 3a. The notebook

Scout learns between runs through a notebook issue, described in `.claude/notebook.md`.
Create the `notebook` label once (the command is in that file); Scout creates the issue
itself on its first run. Pin it. When it passes about 25 lessons, run
`/scout:run <notebook issue number>` and Scout folds the durable lessons into its
playbook by pull request.

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
