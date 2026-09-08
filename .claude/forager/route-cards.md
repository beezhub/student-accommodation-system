# Route cards

A route card is an issue that Forager can fly without making a single decision of its own.
Writing one is the whole of the human's job. Once it is written, the work is mechanical.

An issue becomes a route card when a person adds the `forager:route` label. The label is a
claim that the four questions below are answered in the issue body. Forager checks that
claim before it starts, and hands the issue back if it does not hold.

---

## The four questions

### 1. The sites. Where does the change happen?

Either an explicit list, or a command that produces the list. A command is better,
because Forager can run it and you can both see the same answer.

```
grep -rn "OFFER_EXPIRY_DAYS" backend/src/main/resources/application.yml
```

Not "everywhere this pattern appears". Not "the affected files". If you cannot say where,
the work is still exploration and it belongs to Scout or to you.

### 2. The change. What happens at one site?

One worked example, written out in full. The best route cards apply the change to the
first site by hand and point at that commit: "do what `a1b2c3d` did to `db`, to every
service in the list".

Forager copies the shape. It does not interpret the intent behind it.

### 3. The check. How do you know one site is done?

A command that passes or fails. `./gradlew test`, `npm run lint`, or something narrower
and faster like a `docker compose config` render or a single grep that should come back
empty at the end.

"Looks right" is not a check.

### 4. Done. When is the source empty?

Usually: the command in question 1 returns nothing, or every site in the list is ticked.
Say it in one line so there is no argument about whether the run finished.

---

## Template

```markdown
## Route card

**Sites**

<a command that lists them, or an explicit list>

Expected count: <roughly how many, so Forager can tell if it found the wrong thing>

**The change**

At each site, <the change in one or two plain sentences>.

Worked example, already applied at <site>:

    <the before and after, or a link to the commit>

**The check**

    <command>

**Done when**

<one line>

**Known odd ones out**

<any site you already know will not fit, and what Forager should do: skip it, or leave
it exactly as it is. If you do not know of any, say "none known" so Forager knows you
thought about it.>
```

---

## What makes a bad route card

**"Fix the env var forwarding."** No sites, no shape, no check. This is a job, not a route.

**"Update the deprecated calls where it makes sense."** The judgement is in the card, so
the judgement lands on Forager, so it is not a route card. Decide first, then write it.

**"Rename the field and update the callers."** Two different changes. A rename in the
entity and an update at each caller are different shapes. Split it: do the first by hand,
then hand Forager the callers.

**A card with four sites.** Not wrong, just wasted. Under about five sites, Scout can do
it as an ordinary issue and the review is easier. Forager earns its keep from ten sites
upward, where the value is in the change being provably identical every time.

---

## Good candidates in this repository

Written down because they come up again and again, and because two of them are sitting
in closed issues right now waiting for someone to cut them properly.

- **Forwarding environment variables into the compose services.** `application.yml` reads
  roughly 30 variables that no compose file passes through, so setting them in `.env`
  does nothing. One line per variable, same shape every time, one check that renders the
  config. This is the route card the closed #246 has been waiting for.
- **A single rename across callers**, once the definition has been renamed by hand.
- **Clearing a catalogued lint or styling debt**, where the catalogue already exists.
  `frontend/docs/STYLING_AUDIT_REPORT.md` is that kind of list.
- **Adding a missing annotation or guard at a set of sites a person has audited**, for
  example `TenantOwnershipGuard.assertOwned` at admin paths that load by id. Note this
  one touches tenancy, so it is a hand-back under Forager's own limits unless a person
  has reviewed each site first and says so in the card.

## When not to reach for Forager

If you find yourself writing the card and discovering the answer as you go, stop. You are
exploring, and that is Scout's work or yours. The card is meant to be a record of a
decision you have already made, not the place you make it.
