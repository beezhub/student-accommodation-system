---
name: "Forager: Status"
description: Show the route queue, what Forager is flying, and what it skipped. Changes nothing.
category: Workflow
tags: [issues, automation, forager]
---

Report on Forager without changing anything. Read only. Do not edit code, labels, or comments.

Gather from `beezhub/student-accommodation-system`:

1. Open issues labelled `forager:working` - the route Forager believes it is flying now.
   Read its claim comment for the site count.
2. Open issues labelled `forager:route` - the queue, oldest first. This is the most
   useful section, because an empty queue is the usual reason Forager did nothing.
3. Open issues labelled `forager:in-review` and Forager's open pull requests, with CI
   state. Mark any that also carry `forager:partial`, and say how many sites were skipped.
4. Open issues labelled `forager:needs-human` - cards handed back as unflyable. For each,
   say in one line which of the four questions was missing.

Present it like this, and keep it short:

```
## Forager status

**Flying now**
#301 Forward the backend settings that reach no container - 28 sites, claimed 20 minutes ago

**Queue**
#305 Add the missing tenant guard at the audited admin paths (2 days old)

**In review**
#302 chore(compose): forward the backend settings - CI green, 3 sites skipped

**Handed back**
#298 Clear the stylesheet budget debt - no check named, so nobody can tell when a site is done
```

Then, in one or two sentences: is anything stuck, and what is the single most useful
thing the user could do right now. If the queue is empty, say that plainly and say that
writing a route card is the only thing that will give Forager work, pointing at
`.claude/forager/route-cards.md`.

If a pull request is marked `forager:partial`, say how many sites were skipped and that
those need a person's decision. A partial route is the normal outcome, not a failure,
but the skipped sites do not resolve themselves.

If a label is missing from the repository, say which one and point at
`.claude/forager/setup.md`. Do not create it yourself.
