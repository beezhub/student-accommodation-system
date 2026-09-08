# Forager

Forager is the second agent on this repository. It does one thing: takes a change you
have already decided on, and applies it at every place that needs it, in one pull request.

The name comes from forager bees. A scout finds the source and comes back to report.
The forager flies the route it was told about, works it until it is empty, and hauls the
load home. Same flight, many times, no deciding.

That split is the point. Scout explores and is deliberately forbidden from touching more
than about five files, because a wide change made on its own judgement is a change nobody
can review. Forager is allowed the wide change precisely because the judgement is not its
own: you made it once, in writing, before the run started.

## Route cards

Forager will not lift a finger without a route card. A route card is an issue, labelled
`forager:route`, that answers four questions:

1. **The sites.** Where does the change happen? A list, or better, a command that produces
   the list.
2. **The change.** What happens at one site? One worked example, in full.
3. **The check.** What command proves a site is done?
4. **Done.** When is the source empty?

Writing that card is your whole job. If you find yourself working out the answer as you
write, stop: you are still exploring, and that is Scout's work or yours.

Templates and worked examples are in `.claude/forager/route-cards.md`.

## What a run looks like

1. Checks its own open pull requests and fixes any that are failing or have comments.
2. Takes the oldest unclaimed route card.
3. Checks the card against the four questions. If one is missing, it hands the card back
   and stops.
4. Claims the issue and posts the list of sites it found. That list is the contract for
   the run.
5. Applies the same change at every site, skipping any that do not fit.
6. Runs the check across all of them.
7. Opens one pull request listing the sites changed and the sites skipped.

One route per run. Never more.

## The skipped list

Forager will reach sites where the change does not quite fit, and it is under orders to
skip them rather than improvise. Those sites go in the pull request under their own
heading, and the issue gets a `forager:partial` label.

**That list is the most useful thing Forager produces.** A sweep where every hunk is
identical can be reviewed by reading one hunk and trusting the rest. The skipped sites
are the only part that needs a person, and they are pulled out so you can find them.

A route with zero skips across a large site list is worth a second look. Either the
codebase is unusually consistent, or something got adapted that should not have been.

## Spotting Forager on an issue

Every comment Forager writes opens with 🧺. Scout uses 🔭. You can tell the two apart,
and both apart from a colleague, without reading the thread.

That mark stays in comments. Commits and pull requests carry nothing of the sort: they
are ordinary contributions, authored under the repository's configured user, and they
read that way.

## What it will not do

- It never chooses its own work. No route card, no run.
- It never varies the change between sites, and never widens the site list once posted.
- It never merges anything. Every change waits for a person.
- It never pushes to `main` or `develop`, and never force-pushes.
- It never touches secrets, keys, or `.env` files.
- It never edits an existing database migration, and never deletes or migrates live data.
- It never deletes or skips a test to get a green build.
- It never touches an issue carrying a `scout:` label.
- It never keeps more than three of its own pull requests open. Sweep pull requests are
  wide, and a queue of them helps nobody.

## What it hands back

Forager returns a card unflown when the card is missing one of the four answers, when the
site list it finds does not match what the card claims, when more than about a quarter of
the sites need skipping, or when the change would touch the database schema, an API the
frontend depends on, login, tenancy, money, or personal data. Wide and mechanical does not
make those safe.

There is no waiting-for-an-answer state. Either a card is flyable or it comes back to you
to redraw. Forager does not hold a conversation halfway through a sweep.

## Steering it

| You want | Do this |
|---|---|
| Forager to do a sweep | Write a route card and label it `forager:route` |
| Forager to stay away from an issue | Add the label `forager:hands-off` |
| To know what is queued | Run `/forager:status` in Claude Code |
| One route right now | Run `/forager:run` in Claude Code |
| One specific card, right now | Run `/forager:run 301` |
| To stop it entirely | Set the `FORAGER_ENABLED` repository variable to `false` |

An empty queue is the normal state. Forager is a tool you reach for after deciding
something, not a process that runs on its own.

## Turning it on

Full instructions in `.claude/forager/setup.md`. In short: create the six labels, then run
it by hand from Claude Code or the Actions tab. The daily GitHub Actions workflow at
`.github/workflows/forager.yml` is off until you set the `ANTHROPIC_API_KEY` secret and
the `FORAGER_ENABLED` repository variable, and #287 is worth reading before you turn on
any scheduled agent.

## Forager and Scout together

They must never share an issue. Forager skips anything with a `scout:` label, Scout skips
anything with a `forager:` label, and neither removes the other's. If a piece of work
needs deciding and then sweeping, that is two issues: Scout's, and then a route card that
references it.

The scheduled runs are offset and have separate concurrency groups, so they cannot start
at the same time.

## The files behind it

| File | What it is |
|---|---|
| `.claude/agents/forager.md` | Who Forager is and the rules it cannot break |
| `.claude/forager/route-cards.md` | What a route card must contain, with templates |
| `.claude/forager/playbook.md` | The step-by-step run |
| `.claude/forager/setup.md` | Labels, scheduling, and the off switch |
| `.claude/commands/forager/run.md` | The `/forager:run` command |
| `.claude/commands/forager/status.md` | The `/forager:status` command |
| `.github/workflows/forager.yml` | The daily runner, off by default |

If you want Forager to behave differently, edit the playbook. If you want it to accept
different work, edit the route card rules. Those two do most of the work.
