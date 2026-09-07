---
name: "Scout: Status"
description: Show what Scout is working on, waiting for, and has open. Changes nothing.
category: Workflow
tags: [issues, automation, scout]
---

Report on Scout without changing anything. Read only. Do not edit code, labels, or comments.

Gather from `beezhub/student-accommodation-system`:

1. Open issues labelled `scout:working` - what Scout believes it is on right now.
2. Open issues labelled `scout:needs-input` - questions waiting on a person. For each,
   say what the question was in one line and how many days it has been sitting.
3. Open issues labelled `scout:in-review` and Scout's open pull requests, with CI state.
4. Open issues labelled `scout:ready` - queued up for the next run.

Present it like this, and keep it short:

```
## Scout status

**Working on now**
#268 Documents will not load - claimed 40 minutes ago

**Waiting on you**
#267 Should inactive tenants disappear from the picker, or show as greyed out? (2 days)

**In review**
#271 fix: serve presigned document URLs on the public host - CI green

**Queued next**
#265 Rewrite How It Works styling mobile-first
```

Then, in one or two sentences: is anything stuck, and what is the single most useful
thing the user could do right now. If a question has been waiting more than seven days,
say so plainly, because Scout will stand down from it.

If a label is missing from the repository, say which one and point at
`.claude/scout/setup.md`. Do not create it yourself.
