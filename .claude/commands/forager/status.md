---
name: "Forager: Status"
description: Show what Forager has filed, what is waiting on a person, and what Scout has taken. Changes nothing.
category: Workflow
tags: [issues, audit, automation, forager]
---

Report on Forager without changing anything. Read only. Do not edit code, labels, or comments.

Gather from `beezhub/student-accommodation-system`:

1. Open issues labelled `forager:found`. For each: title, severity, lens (from the first
   line of the body), and how many days it has waited for a person.
2. Among those, any that also carry `scout:ready`, `scout:working`, or `scout:in-review`.
   Those have been released and Scout has them; list them separately.
3. Issues closed in the last seven days that carried `forager:found`, split into closed by
   Forager (problem gone) and closed by a person.
4. Which lens today's run would use, from the weekday table in `.claude/forager/playbook.md`.

Present it like this, and keep it short:

```
## Forager status

**Waiting for a person to release or close** (Scout skips these)
#301 Five application.yml variables are missing from .env.example - low, docs-config, 2 days
#302 Seven services carry class-level @Transactional - low, backend, 1 day

**Released to Scout**
#299 Building images are stored with the client-declared content type - scout:in-review

**Closed this week**
Forager closed 1 (problem gone on develop). People closed 2.

**Today's lens:** frontend (Wednesday)
```

Then, in one or two sentences: is anything piling up, and what is the single most useful
thing the user could do right now. If ten or more `forager:found` issues are open, say so
plainly, because Forager will stop filing until some are released or closed.

If the `forager:found` label does not exist in the repository, say so and point at
`.claude/forager/setup.md`. Do not create it yourself.
