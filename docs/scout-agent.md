# Scout

Scout is an agent that works through GitHub issues on this repository. It runs on a
schedule, picks up one issue at a time, fixes it, and opens a pull request for a person
to review. When it hits something that is not its call to make, it stops and asks you
in the issue, in plain language.

The name comes from scout bees. They leave the hive, find one good site, and come back
to report. That is the whole job.

## What a run looks like

Once an hour, Scout wakes up with no memory of last time and does this:

1. Checks whether anyone answered a question it asked, and picks that work back up.
2. Checks its own open pull requests, and fixes any that are failing or have comments.
3. Picks one open issue, if there is a sensible one.
4. Reads the code, and decides whether the fix is genuinely small.
5. Either makes the change, or asks you a question and stops.
6. Runs the tests, opens a pull request, and comments on the issue.
7. Reports what it did in a few plain sentences.

One issue per run. Never more.

## Spotting Scout on an issue

Every comment Scout writes opens with 🔭, so you can tell its voice from a
colleague's without reading the whole thread.

That mark stays in comments. Its commits and pull requests carry nothing of the sort:
they are ordinary contributions, authored under the repository's configured user, and
they read that way.

## What it will not do

- It never merges anything. Every change waits for a person.
- It never pushes to `main` or `develop`, and never force-pushes.
- It never touches secrets, keys, or `.env` files.
- It never edits an existing database migration, and never deletes or migrates live data.
- It never deletes or skips a test to get a green build.
- It never keeps more than five of its own pull requests open. At five it stops taking
  new work and spends its runs getting the existing ones over the line.

## What it stops and asks about

Scout asks instead of guessing when the issue could be read two ways, when it cannot
reproduce the problem, when the fix spreads past about five files, or when the change
would touch the database schema, an API the frontend relies on, login, tenancy, money,
or personal data.

Questions go in the issue itself. They are written for someone who is not necessarily a
developer: what Scout was doing, what it found, the one question, two or three options
with a recommendation, and what happens if nobody replies. Answer in the issue and Scout
picks it up on its next run.

## Steering it

| You want | Do this |
|---|---|
| Scout to do this issue next | Add the label `scout:ready` |
| Scout to stay away from an issue | Add the label `scout:hands-off` |
| Scout to take an issue Forager filed | Add `scout:ready`, or remove `forager:found` (see `docs/forager-agent.md`) |
| To know what Scout is doing | Run `/scout:status` in Claude Code |
| One run right now | Run `/scout:run` in Claude Code |
| One specific issue, right now | Run `/scout:run 268` |
| To stop it entirely | Set the `SCOUT_ENABLED` repository variable to `false` |

When Scout stops, read which label it left. `scout:needs-input` means it is waiting on an
answer from you and will continue. `scout:needs-human` means it has handed the work back:
too big, or a decision it should not make alone. That one is a job to schedule, not a
question to answer.

Labels Scout sets for itself: `scout:working` while it is on an issue, `scout:needs-input`
while it is waiting on you, and `scout:in-review` once a pull request is open. Those labels
are its only memory, so leave them be.

## Turning it on

Full instructions in `.claude/scout/setup.md`. In short: create the five labels, then
either ask for an hourly Routine in Claude Code on the web, or set the `ANTHROPIC_API_KEY`
secret and the `SCOUT_ENABLED` repository variable to run the hourly GitHub Actions
workflow at `.github/workflows/scout.yml`.

Watch two or three runs before you leave it alone.

## The files behind it

| File | What it is |
|---|---|
| `.claude/agents/scout.md` | Who Scout is and the rules it cannot break |
| `.claude/scout/playbook.md` | The step-by-step run |
| `.claude/scout/asking-for-help.md` | How it writes questions, with templates |
| `.claude/scout/setup.md` | Labels, scheduling, and the off switch |
| `.claude/commands/scout/run.md` | The `/scout:run` command |
| `.claude/commands/scout/status.md` | The `/scout:status` command |
| `.github/workflows/scout.yml` | The hourly runner, off by default |

If you want Scout to behave differently, edit the playbook. If you want it to talk
differently, edit the asking-for-help file. Those two do most of the work.
