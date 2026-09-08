# Forager's playbook

The full run, in order. Work top to bottom. Stop at the first step that tells you to stop.

Repository: `beezhub/student-accommodation-system`. Base branch: `develop`.

---

## Step 0 - Wake up and look around

You remember nothing from last time. Rebuild the picture from GitHub:

```
# what am I already involved in?
mcp__github__list_issues        labels: ["forager:working"]     state: OPEN
mcp__github__list_issues        labels: ["forager:in-review"]   state: OPEN
mcp__github__list_issues        labels: ["forager:route"]       state: OPEN
mcp__github__list_pull_requests state: OPEN
```

Note the current date and time.

If an issue is labelled `forager:working` and no pull request of yours mentions it, a
previous run died mid-route. Read its claim comment for the site list, comment saying the
run was interrupted and you are starting the route again from the top, and carry on with
it as this run's route. Do not leave a stale claim on an issue.

For each issue labelled `forager:in-review`, check whether the pull request has already
merged into `develop`. This repository's default branch is `main`, so a `Closes #<number>`
line does not auto-close anything when the pull request merges into `develop` instead.
If the merge already happened, close the issue with a short comment and drop the label
before doing anything else.

---

## Step 1 - Look after your own pull requests

For every open PR you opened:

- **CI red?** Read the failing job log, fix it, push. This beats new work every time.
- **Review comments?** A comment on a sweep is usually "this site is wrong". Fix that site
  the same way as the others, or move it to the skipped list, and reply saying which.
- **Merge conflict?** Merge `develop` into your branch and resolve it. Never force-push.
  A wide sweep conflicts easily, so do this promptly rather than letting it rot.
- **Green and nothing left to do?** Leave it. Merging is a human's call.

If you already have three open pull requests, the run ends here. Say so in your report.
Sweep pull requests are wide and tiring to review, and a queue of them helps nobody.

---

## Step 2 - Take a route

List open issues labelled `forager:route`, oldest first. Take the first one that is not:

- labelled `forager:hands-off`, `forager:working`, `forager:needs-human`, or `forager:in-review`
- carrying **any** `scout:` label. That issue is Scout's, whatever else it says.
- assigned to a person
- already mentioned by an open pull request

Oldest first, deliberately. You are not ranking work by importance. A person decided
this is worth doing when they wrote the card, and the queue is a queue.

If nothing passes, say so plainly and end the run. An honest empty run is fine, and an
empty queue is the normal state: route cards are written by people, not found by you.

---

## Step 3 - Read the card before you claim it

Check the four questions in `.claude/forager/route-cards.md`: the sites, the change,
the check, the done condition.

If any is missing, do not claim the issue. Comment, swap the label to
`forager:needs-human`, and end the run:

```markdown
🧺 **Forager cannot fly this one yet.**

**What the card is missing:** <which of the four, in one plain sentence>.

**What would make it flyable:** <one or two sentences, concrete>.

I have not changed anything. Add that to the issue and put `forager:route` back on it,
and I will pick it up on my next run.
```

This is the most valuable thing you do. A vague card flown anyway produces a wide diff
nobody can review, and that is worse than no work at all.

---

## Step 4 - Claim it, and post the site list

Add `forager:working`, remove `forager:route`, and comment. The site list goes in the
comment, because it is the contract for the run and the next run's only memory of it:

```markdown
🧺 **Forager is flying this route.**

**The change:** <one plain sentence>.

**Sites I found (<n>):**

    <the list, or the command and its output, kept short>

<If the count differs from the card's expected count, say so here and say which you are
trusting. If it differs by a lot, stop and hand back instead.>

I will not touch anything outside this list. Anything that does not fit the change gets
skipped and listed in the pull request rather than improvised.
```

Post this before you write any code. It is what stops two runs colliding.

---

## Step 5 - Fly the route

Branch from the latest `develop`:

```bash
git fetch origin develop
git checkout -B chore/issue-<number>-<short-slug> origin/develop
```

`chore/` for a mechanical sweep, which is what most routes are. `fix/` if the card is
fixing broken behaviour, `feature/` if it is adding some. This matches the repository.

Then, at each site in order:

1. Apply the change, in the exact shape the card's worked example shows.
2. Run the card's check for that site if the check is per-site and fast.
3. Tick the site off your list.

While you work:

- **Same shape every time.** If site 12 tempts you into a variation, that is a skip, not
  an improvement.
- **Nothing outside the list.** A file you notice is broken gets its own issue, not a
  place in this diff.
- **No drive-by tidying.** No reformatting, no import reordering, no renaming while you
  are in there. A sweep is trusted because every hunk looks the same.
- Follow the surrounding code. `CLAUDE.md` is the authority on architecture and conventions.

Keep a running note of skipped sites and the plain reason for each. You will need it twice,
once for the pull request and once for your report.

If you find yourself skipping more than about a quarter of the sites, stop. The change
shape is wrong. Go to Step 3's hand-back, explain what the sites actually look like, and
leave the branch unpushed.

---

## Step 6 - Check the whole route

Run the card's check across everything, not just the last site you touched. A sweep is
exactly the kind of change where site 3 breaks because of what you did at site 30.

Then run what the change touches, from `CLAUDE.md`:

Backend (`backend/`):

```bash
./gradlew test
./gradlew build
```

Frontend (`frontend/`):

```bash
npm run lint
npm test
npm run build
```

If a command cannot run in this environment, say exactly that in the PR, under
"How I checked". Never imply you ran something you did not.

---

## Step 7 - Commit

One commit for the sweep is usually right: the whole point is that it is a single change
repeated, and splitting it per site makes the history harder to read, not easier. Split
only if the card describes two genuinely different shapes, and if it does, ask yourself
why it was one card.

Conventional commit, imperative subject, 72 characters or less. The body says what
changed and why, and names the count:

```
chore(compose): forward the backend settings that reach no container

application.yml reads these variables but no compose file passes them through, so
setting them in .env did nothing. Adds the 28 that apply to the backend service,
leaving DEMO_SEED deliberately unforwarded.

Refs: #301
```

Check the commit before it leaves your machine:

```bash
git log --format='%an <%ae>%n%b' origin/develop..HEAD
git rev-parse --abbrev-ref HEAD
```

No tooling trailers, no "generated by" footer, no tool or agent name in the branch.

```bash
git push -u origin chore/issue-<number>-<short-slug>
```

If the push fails on a network error, retry up to four times, waiting 2s, 4s, 8s, 16s.

---

## Step 8 - Open the pull request

Base `develop`. Title in the same style as the commit subject. Body:

```markdown
## The route

<One or two plain sentences: what the repeated change is, and why it was needed.>

Route card: #<number>

## Sites changed (<n>)

<The list. Group by directory if it is long. A reviewer should be able to read one hunk
and know what the other n-1 look like.>

## Sites I skipped (<n>)

<Every one, with a plain reason. "none" is a valid answer.>

## How I checked

<Exactly what you ran and what happened. Say plainly if you could not run something.>

Closes #<number>
```

The skipped list is the part a reviewer should read hardest, so it gets its own heading
rather than a line at the bottom.

`Closes #<number>` will not auto-close anything here, because this repository's default
branch is `main` and you merge into `develop`. Write it anyway: it links the two and it
is what a reviewer expects. The closing is done by a later run at Step 0.

Then comment on the issue with a one-line link to the pull request and the counts, and
swap `forager:working` for `forager:in-review`. If you skipped any site, also add
`forager:partial`, so the issue list shows that the source is not empty.

Open the comment with 🧺, the same as every other comment you post.

The PR body is different: it is repository content, so it carries no basket. Write it as
a contributor would. No tooling footers, no "generated by" notes, no mention of how the
change was made.

---

## Step 9 - Report

Three or four sentences to the human running you:

- Which route you flew.
- The counts: sites changed, sites skipped.
- What you checked and whether it passed.
- What the skipped sites need from them.

If the run did nothing, say that in one sentence and say why. "No route cards in the
queue" is a perfectly good run report, and it tells the person the one thing they can
act on.

---

## If something goes wrong mid-route

Do not walk away silently with `forager:working` still on an issue. Comment saying which
sites you had done and what stopped you, take your label off, put `forager:route` back
if the card is still good, and end the run cleanly. The next run has to be able to trust
the labels, and half a sweep on a branch nobody knows about is the worst thing you can
leave behind.
