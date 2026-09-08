---
name: "Forager: Run"
description: Do one Forager pass - audit one lens of the codebase and file up to three verified issues
category: Workflow
tags: [issues, audit, automation, forager]
---

Do one Forager pass over `beezhub/student-accommodation-system`.

Launch the `forager` subagent with this instruction:

> Do one full run following `.claude/forager/playbook.md`, start to finish.
>
> Remember you have no memory of previous runs. Rebuild the picture from the open issues
> labelled `forager:found`, their comments, and the open pull requests before you decide
> anything.
>
> Order, always:
> 1. Re-check every open `forager:found` issue against the current `develop`. Close the
>    ones whose problem is gone. Answer a direct question a person asked, once.
> 2. Audit today's lens from `.claude/forager/lenses.md`.
> 3. Verify, rule out known debt and duplicates, then file at most three issues using
>    `.claude/forager/writing-issues.md`.
>
> You never change a file, never commit, never push, never open or comment on a pull
> request. If you find yourself about to, stop and write the issue instead.
>
> Finish with a report of three or four plain sentences: which lens, what you closed, what
> you filed with issue numbers, and anything you saw outside the lens that a person should
> look at.

If the user named a lens (for example `/forager:run tenancy`), tell Forager to use that
lens instead of the weekday's. `/forager:run housekeeping` means Step 1 only, no new issues.
The lens names are the section headings in `.claude/forager/lenses.md`; if the user's word
matches none of them, ask which they meant rather than guessing.

When Forager is done, relay its report to the user in plain language with links to any
issues it filed. Do not repeat the whole transcript.
