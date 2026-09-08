---
name: "Forager: Run"
description: Fly one Forager route - take a route card, apply the same change at every site, open a pull request
category: Workflow
tags: [issues, automation, forager]
---

Fly one Forager route over `beezhub/student-accommodation-system`.

Launch the `forager` subagent with this instruction:

> Fly one full route following `.claude/forager/playbook.md`, start to finish.
>
> Remember you have no memory of previous runs. Rebuild the picture from GitHub labels,
> issue comments, and your open pull requests before you decide anything.
>
> Order of priority, always:
> 1. Your own open pull requests that are failing CI or have review comments.
> 2. The oldest unclaimed issue labelled `forager:route`.
>
> Check the route card against the four questions in `.claude/forager/route-cards.md`
> before you claim anything. If the sites, the change, the check, or the done condition
> is missing, comment saying which one, label it `forager:needs-human`, and stop. Do not
> fill the gap yourself.
>
> One route only. Same change at every site. A site that does not fit gets skipped and
> listed, never improvised.
>
> Finish with a report of three or four plain sentences: which route, how many sites you
> changed, how many you skipped and why, what you checked, and what the skipped sites
> need from a person.

If the user named an issue (for example `/forager:run 301`), tell Forager to fly that card
and skip the selection step, but keep every other step including the four-question check.
If the card turns out to be incomplete, Forager still hands it back rather than guessing.

If there are no route cards in the queue, that is a normal result, not a failure. Say so
in one line and point the user at `.claude/forager/route-cards.md` if they want to write
one.

When Forager is done, relay its report to the user in plain language, lead with the
counts, and add the pull request link if there is one. List the skipped sites: that is
the part the user has to act on. Do not repeat the whole transcript.
