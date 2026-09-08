# Setting Forager up

Forager does mechanical sweeps: one change, already decided by a person, repeated across
every site a route card names. It is the counterpart to Scout, which explores and is
forbidden from touching more than about five files.

Read `.claude/forager/route-cards.md` first. Writing the card is the human's whole job,
and Forager is useless without one.

## 1. The labels Forager uses

Forager has no memory between runs. Labels are how it knows what happened last time,
and how you tell it what to do. Create these once:

| Label | Colour | Who sets it | Meaning |
|---|---|---|---|
| `forager:route` | `#0e8a16` | You | This issue is a route card. Forager may fly it. |
| `forager:hands-off` | `#b60205` | You | Forager must never touch this issue. |
| `forager:working` | `#fbca04` | Forager | Forager is flying this route right now. |
| `forager:needs-human` | `#5319e7` | Forager | The card is not flyable. A person has to redraw it. |
| `forager:in-review` | `#1d76db` | Forager | Forager has opened a pull request for this. |
| `forager:partial` | `#c5def5` | Forager | The route was flown but sites were skipped. See the PR. |

```bash
gh label create forager:route       --color 0e8a16 --description "A complete route card, Forager may fly it"
gh label create forager:hands-off   --color b60205 --description "Forager must not touch this"
gh label create forager:working     --color fbca04 --description "Forager is flying this route now"
gh label create forager:needs-human --color 5319e7 --description "The route card is not flyable as written"
gh label create forager:in-review   --color 1d76db --description "Forager has opened a PR for this"
gh label create forager:partial     --color c5def5 --description "Route flown, some sites skipped"
```

`forager:route` is the one that matters. Without it Forager will never start, which is
deliberate: an agent that picks its own sweeps is not a forager.

There is no `forager:needs-input`. Forager does not hold a conversation mid-route. Either
the card is flyable or it comes back to you to redraw.

## 2. Keeping Forager and Scout out of each other's way

They share a repository and must not share an issue.

- Forager skips any issue carrying a `scout:` label, and never removes one.
- Scout skips any issue carrying a `forager:` label, and never removes one.
- Never put both entry labels on one issue. If work needs deciding then sweeping, that is
  two issues: Scout's, then a route card that references it.

The scheduled runs are offset by half an hour for the same reason, and each has its own
concurrency group.

## 3. Running Forager once, by hand

In Claude Code:

```
/forager:run
```

One route, start to finish. To fly a specific card:

```
/forager:run 301
```

To see what it is up to without changing anything:

```
/forager:status
```

That also lists route cards waiting in the queue, which is usually what you want to know.

## 4. Running Forager on a schedule

Forager is a poor fit for a tight schedule. Its queue is written by hand, so most runs
would wake up, find nothing, and cost you Actions minutes to say so. Prefer running it
when you have just written a card.

If you do schedule it, pick one of these. Do not run two at once.

### Option A: a scheduled session

In Claude Code on the web, ask for a Routine that runs `/forager:run`. Daily is plenty.

### Option B: your own machine

```
/loop 1h /forager:run
```

Runs while that Claude Code session is open.

### Option C: GitHub Actions

`.github/workflows/forager.yml` is in this repository, set to run once a day, and
switched off until you turn it on:

1. Add a repository secret `ANTHROPIC_API_KEY`.
2. Add a repository variable `FORAGER_ENABLED` set to `true`.

Take the variable away and Forager stops, without deleting anything. You can also run it
by hand from the Actions tab at any time, which is the way it is meant to be used.

Note the Actions minutes question in #287 before turning on any scheduled agent.

## 5. Turning Forager off

- **One issue:** put `forager:hands-off` on it, or take `forager:route` off it.
- **Everything, right now:** set `FORAGER_ENABLED` to `false`, or stop the Routine or loop.
- **Permanently:** delete `.claude/agents/forager.md`.

Forager never merges anything. Every change waits for a person, so the worst an unattended
Forager can do is open a pull request nobody wants.

## 6. What to check in the first week

- **Are the diffs boring?** They should be. Every hunk in a sweep ought to look like every
  other hunk. If a pull request needs reading line by line, the card was too loose.
- **Is the skipped list honest?** A sweep with zero skips on a large site list is worth a
  look: either the codebase is very consistent, or Forager improvised. Read three hunks
  at random.
- **Is the queue empty most days?** That is normal and not a problem. Forager is a tool
  you reach for, not a process that runs on its own.
