# Setting Forager up

## 1. The one label Forager uses

| Label | Colour | Who sets it | Meaning |
|---|---|---|---|
| `forager:found` | `#c5def5` | Forager | Forager filed this after a scheduled audit. Nobody has reviewed it yet. Scout skips it until a person releases it. |

Create it from the GitHub label screen, or:

```bash
gh label create forager:found --color c5def5 --description "Filed by Forager after an audit; not yet reviewed by a person"
```

Forager also uses the labels the repository already has: `bug`, `enhancement`,
`severity:*`, and `area:*`. It never creates one. If an area is missing a label, create the
label and Forager will start using it.

## 2. Releasing an issue to Scout

Forager writes issues; it does not decide that they should be fixed. A person does, in one
of three ways:

| You think | Do this |
|---|---|
| Yes, Scout should do this next | Add `scout:ready`. Scout prefers it over everything else. |
| Yes, whenever Scout gets to it | Remove `forager:found`. Scout treats it like any other issue. |
| No, or not now | Close it. Forager will not refile a closed issue unless the problem comes back after a fix. |

Until you do one of those, Scout leaves it alone. That is the whole safety valve: an agent
that writes issues for an agent that fixes them is a loop, and you are the break in it. If
you later trust Forager enough to remove the break, delete the `forager:found` bullet from
Step 3 of `.claude/scout/playbook.md`.

## 3. Running Forager once, by hand

In Claude Code:

```
/forager:run
```

One pass: the lens for today, at most three issues. To choose the lens:

```
/forager:run tenancy
/forager:run housekeeping
```

Lenses: `tenancy`, `backend`, `frontend`, `tests`, `docs-config`, `database`, `housekeeping`.

To see what it has open without changing anything:

```
/forager:status
```

Run it by hand for a week, one lens a day, before you leave it unattended. Read every
issue it files. If they are not the kind of issue you would have written, fix the lens in
`.claude/forager/lenses.md` before you schedule it.

## 4. Running Forager every day

One run a day is enough. The lens rotates with the weekday, so a daily run covers the whole
system every week. Do not run it hourly: the same lens would refile nothing and burn the
budget doing it.

In Claude Code on the web, ask for a Routine that runs `/forager:run` once a day, early
morning UTC. Each run is a fresh session, which is what Forager is built for.

Forager does not run on GitHub Actions, for the same reason Scout does not: the
repository's Actions minutes are metered and near the allowance (GH-284), and a Routine
costs none. The rule that Forager never pushes and never touches a pull request is
enforced by its definition and its tool list, not by a workflow token.

## 5. Turning Forager off

- **One lens:** delete or comment out its section in `.claude/forager/lenses.md`. The
  weekday table in the playbook then lands on an empty lens and the run reports nothing.
- **Everything, right now:** stop the Routine.
- **Permanently:** delete `.claude/agents/forager.md`.

Nothing Forager does needs unwinding. It has never pushed a commit, so turning it off
leaves only issues, and you can close those.

## 6. What to check in the first week

- **Would you have filed it?** If most issues are things you would have closed, the bar in
  `writing-issues.md` is too low or a lens is too eager.
- **Can Scout act on it without asking?** Open one of Forager's issues with `scout:ready`
  and watch. If Scout asks a question that the body should have answered, add that to the
  template.
- **Is it refiling?** Two open issues about the same thing mean the duplicate search in
  playbook Step 5 needs a stronger hint for that lens.
- **Is it quiet on Sundays?** The housekeeping run should close things and file nothing.
