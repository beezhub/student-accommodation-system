# How Scout asks for help

The person reading your question may not have opened this repository today. They may not
be a developer at all. They are probably reading on a phone between two other things.

Write for that person.

## The five things every question needs

1. **What I was doing.** The issue, in your own plain words, not a copy of the title.
2. **What I found.** The one thing you learned that created the question.
3. **The question itself.** One question. Not three.
4. **The options.** Two or three, each with a plain sentence about what it means for
   people using the system. Say which one you would pick and why.
5. **What happens if nobody answers.** Usually: the issue sits and you move on.

## Rules

- No jargon. If a technical word is genuinely the only word, put the plain meaning
  right after it in brackets.
- Say what the user of the system sees, not what the code does. "Students cannot open
  their documents" beats "the storage client returns an unresolvable host".
- No stack traces, no diffs, no log dumps in the question. Link to the file or the PR.
- Under 150 words. Cut anything that is you showing your working.
- Never ask for a password, a key, or anything secret. Ask the human to set it themselves.
- Ask once. Do not follow up, do not nudge, do not repost.

## Template: a choice to make

```markdown
**Scout needs a decision on this one.**

**What I was doing:** <the issue in one plain sentence>.

**What I found:** <the one fact that created the question, in plain words>.

**What I need to know:** <one question>.

**The options as I see them:**

1. **<Short name>** - <what it means for people using the system>.
2. **<Short name>** - <what it means for people using the system>.

I would go with option 1, because <one plain reason>.

If nobody has a view, I will leave this issue alone and pick up something else.
Reply here and I will carry on next time I run.
```

## Template: too big for me

```markdown
**Scout is standing down on this one, it is bigger than it looks.**

**What I was doing:** <the issue in one plain sentence>.

**Why I stopped:** <the plain reason, for example: this changes how data is stored, so
getting it wrong would affect records that already exist>.

**What I would suggest:** <one or two sentences on the approach a person could take>.

I have not changed any code. Removing my label and leaving this for a person.
```

## Template: I cannot reproduce it

```markdown
**Scout could not make this happen.**

**What I was doing:** <the issue in one plain sentence>.

**What I tried:** <two or three plain bullets>.

Everything behaved normally, so I do not want to guess at a fix.

**What would help:** <the smallest specific thing, for example: the exact page you were on,
roughly what time it happened, and whether you were logged in as a student or an admin>.

Add that here and I will pick this back up.
```

## Template: work in progress, saved for later

```markdown
**Scout has saved what it had so far.**

I started on this and got as far as <plain sentence>. I have put that in a draft
pull request so it is not lost: #<number>.

I stopped because <plain reason>.

Nothing is merged and nothing is affected until someone picks that draft up.
```

## A worked example

Not this:

> Blocked on #268. The presigned URL is generated with the internal endpoint
> `http://minio:9000`, which is unresolvable from the browser context. Options are to
> configure a distinct public endpoint on the S3 client, or to proxy object requests
> through the API. Please advise on the preferred approach.

This:

> **Scout needs a decision on this one.**
>
> **What I was doing:** students and admins cannot open any uploaded document or see any
> building photo. Every one of them fails to load.
>
> **What I found:** the links we hand to the browser point at an address that only exists
> inside our server. The browser has no way to reach it, so nothing loads.
>
> **What I need to know:** how should the browser get to those files?
>
> **The options as I see them:**
>
> 1. **Give the file store its own public web address.** Fastest to do, and files continue
>    to download straight from the store. Needs someone with server access to set the address.
> 2. **Send the files through our own application.** Nothing new to set up, and we get to
>    check permissions on every download, but every file goes through our server, which
>    will be slower for large photos.
>
> I would go with option 1, because it is a configuration change rather than new code,
> and it is the smaller risk.
>
> If nobody has a view, I will leave this issue alone and pick up something else.
