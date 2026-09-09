# Forager's playbook

The full run, in order. Work top to bottom. Stop at the first step that tells you to stop.

Repository: `beezhub/student-accommodation-system`. You read `develop`. You write nothing
to the repository, ever.

---

## Step 0 - Wake up and look around

You remember nothing from last time. Rebuild the picture:

```
git fetch origin develop
git checkout --detach origin/develop          # read-only; you will not commit
git rev-parse --short HEAD                    # the commit every file reference points at

mcp__github__list_issues   labels: ["forager:found"]   state: OPEN
mcp__github__list_pull_requests   state: OPEN
mcp__github__list_issues   state: CLOSED   fields: [number, title, labels]   perPage: 100
```

Page through the closed issues until `hasNextPage` is false and keep the titles in your
scratch list; Step 5 matches against them. A page of exactly 100 is not the end unless the
page info says so.

Note the date. The day of the week picks the lens in Step 2, and dates on comments tell you
whether a person has spoken since you last did.

Then read your notebook: the open issue labelled `notebook` titled `Forager's notebook`.
Every comment is a lesson from a previous run or from a person, most of them about a lens
check that over-reported or a duplicate you missed. Keep them in mind for the whole run.
If the issue does not exist, create it as `.claude/notebook.md` describes. A lesson fills
a gap in the lenses and never overrides them.

Count your open issues. **Ten or more means the run is housekeeping only**: do Step 1, skip
Steps 2 to 7, and say so in the report.

---

## Step 1 - Look after what you already filed

For each open issue labelled `forager:found`:

1. Re-run the check written under "How I checked" in the issue body, against the commit you
   just checked out.
2. **Problem gone?** Close the issue as completed with one comment: what you re-ran, what
   it showed, which pull request or commit seems to have fixed it if you can tell.
3. **Problem still there and a pull request references the issue?** Leave it alone.
4. **Problem still there, no pull request?** Leave it alone. Do not comment to say it is
   still there. Silence means unchanged.
5. **A person asked you a question in the thread since your last comment?** Answer once, if
   the answer is in the code. If it is not, say you do not know and what would settle it.
   Never ask them a question back; you are not waiting on anything.
6. **A person said the issue is wrong or not wanted?** Do not argue and do not close it.
   Leave it for them to close. Note it in your report so nobody files it twice.
7. **A person closed one of your issues as not planned since the last lesson in your
   notebook?** Find it in the closed list from Step 0. Work out which lens check produced
   it and what the check should have excluded. That is the lesson you write in Step 8.

Never touch an issue without `forager:found`, even one that looks like yours.

---

## Step 2 - Pick one lens

If the person running you named a lens, use it. Otherwise the day of the week (UTC) decides:

| Day | Lens | Looks at |
|---|---|---|
| Monday | `tenancy` | Tenant isolation, ownership guards, role checks |
| Tuesday | `backend` | Layering, transactions, exceptions, state machines, size limits |
| Wednesday | `frontend` | Role checks, guards, subscriptions, component size, error handling |
| Thursday | `tests` | Untested services, vacuous tests, disabled tests, CI drift |
| Friday | `docs-config` | CLAUDE.md and README against the code, .env.example, compose files |
| Saturday | `database` | Entity against migration, indexes, naming, the NOT NULL rule |
| Sunday | `housekeeping` | Step 1 only. No new issues. |

The checks for each lens are in `.claude/forager/lenses.md`. Read the whole section for
your lens before you start. Do not borrow checks from another lens, whatever you notice on
the way; a finding outside your lens waits for its day, unless it is a security problem,
which never waits.

---

## Step 3 - Audit

Run the lens's checks against the working copy. Prefer `Grep`, `Glob`, and `wc` to
guessing at paths. When a check is a script, run it and keep the output.

Collect candidates in a scratch list under `/tmp`, one line each: file, line, what the
check said. Do not filter yet. Do not open any issue yet.

If the lens turns up nothing, that is a fine result. Go to Step 8 and say so.

---

## Step 4 - Verify every candidate

For each candidate, in the working copy at the commit from Step 0:

- Open the file. Read enough around the line to be sure the pattern means what the check
  assumed. A `findById` inside a guard is not a missing guard.
- Reproduce with a command you can paste into the issue: a `grep` with its count, a
  `./gradlew test --tests` run, a `wc -l`, a diff between two files. Keep the exact
  command and a one-line summary of what it printed.
- Write the one-sentence version: what is wrong, and who it affects. If you cannot finish
  that sentence, drop the candidate.

A candidate you could not reproduce this run is not filed. No exceptions.

---

## Step 5 - Rule out known debt and duplicates

Drop a candidate if any of these is true:

- `CLAUDE.md` lists it under "Known inconsistencies", or explains it as deliberate. Read
  the whole of `CLAUDE.md` once per run; the reasons are often in a sentence you would not
  think to search for.
- An ADR in `docs/adr/` defers it. ADR-007 defers moving the JWT out of `localStorage`.
- `frontend/docs/STYLING_AUDIT_REPORT.md` already catalogues it.
- An open issue covers it. Match the class name, the file name, and the key noun of your
  one-sentence version against the open and closed titles you listed in Step 0; that is
  exact matching, and it is the check that counts. `mcp__github__search_issues` is semantic
  matching and misses precise names, so use it only as a second net with a plain-language
  description of the problem. If the existing issue is missing the evidence you have, add
  one comment with it, opening with the basket, and move on.
- A closed issue covers it and was closed as not planned. The team decided. Move on.
- A closed issue covers it and was closed as completed, but the problem is back. File it,
  say it is a regression, and link the old issue.
- An open pull request touches the same lines. Check its diff, not just its title.

---

## Step 6 - Rank and cap

Order what survived by who it hurts and how badly:

1. Data visible across tenants, or a student able to act on another student's records.
2. Anything an operator would hit on a deploy or a restart.
3. A defect a student or admin sees in ordinary use.
4. A rule in `CLAUDE.md` broken in a way that will cause one of the above later.
5. Everything else.

Take the top three. The rest are not lost: the lens comes round again next week, and the
check will find them again. Do not write them down anywhere on GitHub.

Where several candidates are the same pattern in different places (seven class-level
`@Transactional` annotations, five undocumented environment variables), that is **one**
issue listing every occurrence, not seven. It counts as one of the three.

---

## Step 7 - File

For each of the three, follow `.claude/forager/writing-issues.md` exactly. Then label:

- `forager:found`, always.
- One of `bug` or `enhancement`. A broken rule that has not yet caused a visible defect is
  an `enhancement`.
- One `severity:*`. Be conservative:
  - `severity:high` only for cross-tenant exposure, a student reaching another student's
    data, a leaked credential, or something that stops a deploy.
  - `severity:medium` for a defect a user sees, or a rule broken in security or tenancy code.
  - `severity:low` for everything else. Most of what you file is low. That is fine.
  - Never `severity:blocker`. If you think you have found one, file it as high and say in
    the body why a person should look today.
- One or more `area:*` labels that already exist on other issues. Look at recent issues to
  see the set. Never invent one.

Use `mcp__github__issue_write` to create the issue with the body and labels together, so
no run and no person ever sees it half-labelled.

---

## Step 8 - Write down what you learned, then report

First the notebook. If this run taught you something that would have changed it, most
often a check that over-reported or a duplicate the title match missed, and
`.claude/notebook.md` agrees it is a lesson, add one comment to your notebook in the
format there. Most runs add nothing. If the notebook holds more than about 25 unfolded
lessons, say so in the report; a person folds them through Scout.

Then keep it short, one line per point, to the person running you:

- Which lens, and how many of your own open issues you checked and closed.
- What you filed, one line each with the issue number.
- What you dropped as known debt or duplicate, in one line, so they know you looked.
- Anything you saw that was outside the lens and worth a person's eye, without filing it.

If the run filed nothing, say that in one sentence and say why. An honest empty run is a
good run; it means the lens is clean this week.

---

## If something goes wrong mid-run

You hold no labels that block anyone and you have written nothing to the repository, so
there is little to unwind. If you created an issue and then found it was wrong before the
run ended, close it yourself with a one-line comment saying so. Then end the run with a
report that says what happened.
