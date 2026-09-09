# Forager

Forager is an agent that finds work for this repository. Once a day it inspects one part of
the system against the rules the project has written down for itself, checks each thing it
finds, and files it as a GitHub issue that a person can read in a minute and Scout can fix
without asking what was meant. It never changes code.

The name comes from forager bees. They go out, find what the hive needs, and bring it back.
They do not build comb.

## Why it exists

Scout fixes issues, and it has been doing that well. But Scout can only work when somebody
has written an issue, and until now every issue in this repository was written by one
person. When that person is busy, Scout idles and the backlog sits at two. Forager is the
other half: Scout empties the backlog, Forager keeps it honest and non-empty.

## What a run looks like

Once a day, Forager wakes up with no memory of last time and does this:

1. Re-checks every issue it filed before. If the problem is gone from `develop`, it closes
   the issue and says what fixed it.
2. Picks the lens for the day of the week: tenancy on Monday, backend conventions on
   Tuesday, frontend on Wednesday, tests and CI on Thursday, docs and configuration on
   Friday, database and migrations on Saturday. Sunday is housekeeping only.
3. Runs that lens's checks against the code and collects candidates.
4. Verifies each one with a command it can show you, and drops anything it cannot reproduce.
5. Drops anything the team already knows about: `CLAUDE.md`'s known inconsistencies, ADRs
   that defer things, open or closed issues, open pull requests.
6. Files at most three issues, ranked by who they hurt, each with the exact place, the exact
   check, why it matters, a suggested fix, and an honest note on whether it is Scout-sized.
7. Reports what it did in a few plain sentences.

Three issues a day at most. If it has ten open that nobody has looked at, it files nothing
until some are released or closed.

## Spotting Forager on an issue

Every issue and comment Forager writes opens with 🧺, so you can tell its voice from a
colleague's and from Scout's 🔭 without reading the whole thing. Its issues also carry the
`forager:found` label.

## How it learns

Forager keeps a notebook, an open issue labelled `notebook` titled `Forager's notebook`,
with one lesson per comment. Its best lessons come from you: when you close one of its
issues as not planned, the next run works out which check produced it and writes down
what to exclude, so the pattern stops being filed. It reads the notebook before every run
and never lets a lesson override the lens file. Forager cannot edit files, so when the
notebook grows long you have Scout fold it into the lenses with `/scout:run <notebook
issue number>`. Details in `.claude/notebook.md`.

## Nothing happens until you say so

Forager writes issues. It does not decide they should be fixed. Scout skips anything
labelled `forager:found`, so a Forager issue sits until a person does one of these:

| You think | Do this |
|---|---|
| Yes, Scout should do this next | Add `scout:ready` |
| Yes, whenever Scout gets to it | Remove `forager:found` |
| No, or not now | Close it |

That is deliberate. An agent that writes issues for an agent that fixes them is a loop, and
a person is the break in it. If you come to trust Forager's judgement, the break is one
line in Scout's playbook and you can remove it.

## What it will not do

- It never edits a file, commits, pushes, or opens a pull request.
- It never merges, approves, or comments on a pull request.
- It never touches an issue it did not open, beyond adding one comment of evidence to a
  duplicate.
- It never touches a `scout:*` label, and never creates a label.
- It never files something it did not reproduce with a command during that run.
- It never pastes a credential or a personal record into an issue, even one it found in
  the repository. It names the file and stops.
- It never reads `.env` files.

## Steering it

| You want | Do this |
|---|---|
| One run right now, today's lens | Run `/forager:run` in Claude Code |
| One run on a specific lens | Run `/forager:run tenancy` |
| Only the re-check of old issues | Run `/forager:run housekeeping` |
| To see what it has open | Run `/forager:status` |
| It to stop filing about one kind of thing | Edit or remove that check in `.claude/forager/lenses.md` |
| It to write issues differently | Edit `.claude/forager/writing-issues.md` |
| To stop it entirely | Stop the Routine |

## Turning it on

Full instructions in `.claude/forager/setup.md`. In short: create the `forager:found` label,
run `/forager:run` by hand for a week and read what it files, then create a daily Routine
at `claude.ai/code/routines` with this repository selected under **Select repositories**.
Without that selection the run has no checkout and audits nothing. Forager does not run on
GitHub Actions; the minutes are metered.

## The files behind it

| File | What it is |
|---|---|
| `.claude/agents/forager.md` | Who Forager is and the rules it cannot break |
| `.claude/forager/playbook.md` | The step-by-step run |
| `.claude/forager/lenses.md` | What each day's lens checks, and the rule each check enforces |
| `.claude/forager/writing-issues.md` | The issue template, the quality bar, and the labels |
| `.claude/forager/setup.md` | The label, scheduling, releasing issues to Scout, the off switch |
| `.claude/notebook.md` | How Scout and Forager learn between runs |
| `.claude/commands/forager/run.md` | The `/forager:run` command |
| `.claude/commands/forager/status.md` | The `/forager:status` command |

If you want Forager to look for different things, edit the lenses. If you want it to write
differently, edit the writing guide. Those two do most of the work.
