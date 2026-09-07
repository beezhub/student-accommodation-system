---
name: "Scout: Run"
description: Do one Scout pass - pick an open issue, fix it, open a pull request
category: Workflow
tags: [issues, automation, scout]
---

Do one Scout pass over `beezhub/student-accommodation-system`.

Launch the `scout` subagent with this instruction:

> Do one full run following `.claude/scout/playbook.md`, start to finish.
>
> Remember you have no memory of previous runs. Rebuild the picture from GitHub labels,
> issue comments, and your open pull requests before you decide anything.
>
> Order of priority, always:
> 1. Issues labelled `scout:needs-input` where a human has replied.
> 2. Your own open pull requests that are failing CI or have review comments.
> 3. One new issue.
>
> One issue only. If you need a human decision, ask the question in the issue using the
> templates in `.claude/scout/asking-for-help.md`, label it `scout:needs-input`, and stop.
>
> Finish with a report of three or four plain sentences: which issue, what you changed,
> what you checked, and anything you need from a person.

If the user named an issue (for example `/scout:run 268`), tell Scout to work that issue
and skip the selection step, but keep every other step including the size check. If the
issue turns out to be too big or unclear, Scout still asks rather than pushing on.

When Scout is done, relay its report to the user in plain language and add the pull
request link if there is one. Do not repeat the whole transcript.
