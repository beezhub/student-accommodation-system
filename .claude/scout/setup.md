# Setting Scout up

## 1. The labels Scout uses

Scout has no memory between runs. Labels are how it knows what happened last time,
and how you tell it what to do. Create these once:

| Label | Colour | Who sets it | Meaning |
|---|---|---|---|
| `scout:ready` | `#0e8a16` | You | Scout should prefer this issue over anything else. |
| `scout:hands-off` | `#b60205` | You | Scout must never touch this issue. |
| `scout:working` | `#fbca04` | Scout | Scout is on it right now. Do not start it yourself. |
| `scout:needs-input` | `#d93f0b` | Scout | Scout has asked a question and is waiting for you. |
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

## 3. Running Scout on a schedule

**Read this before you try.** Running Scout by hand with `/scout:run` is the default, and
it is what produces work today. Scheduling is possible but only one route actually works,
and two obvious-looking ones do not.

### What does not work

**A Routine created by asking Claude to make one.** A Routine created through the agent
tooling cannot have a repository attached to it. Each firing gets an empty container, so
Scout wakes with no repo, no way to read its own instructions, and no way to reach the
code. It either stalls on a permission prompt nobody is there to answer, or runs, searches
for a repo, finds none, and exits. Six of these fired overnight once and did nothing at
all. The permissions file in this repo does not rescue it either: that file lives inside
the repository the session has not cloned, so it cannot authorise the clone that would
fetch it.

### What works

**A Routine created in the Routines UI on claude.ai**, with
`beezhub/student-accommodation-system` attached as a source on `develop`. The container
then arrives with the repo already on disk. This is the only scheduled route that works,
and only you can create it, from the web UI. It bills against your Claude subscription.

**GitHub Actions**, using `.github/workflows/scout.yml`. `actions/checkout` puts the repo
in place natively. Two things to know: GitHub only honours a cron schedule from the
**default branch**, so `scout.yml` has to be on `main`, not just `develop`; and it needs
an `ANTHROPIC_API_KEY`, which is pay-per-token API billing on top of your plan rather
than subscription usage.

### Pick the cadence deliberately

Hourly is almost certainly wrong. Scout takes one issue per run and stops at three open
pull requests, so against a backlog of a dozen issues it reaches that ceiling in three
runs and then pays, every hour, to work out that it should do nothing. One measured empty
run cost about $0.29. Once each weekday morning keeps pace with a backlog this size at a
fraction of the cost.

## 4. Turning Scout off

- **One issue:** put `scout:hands-off` on it.
- **Everything, right now:** set `SCOUT_ENABLED` to `false`, or stop the Routine or loop.
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
