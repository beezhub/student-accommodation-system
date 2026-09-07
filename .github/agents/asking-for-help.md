# How to ask a person for help

When you get stuck, the comment you write is the whole product of that run. If
a person reads it and still has to open the code to understand what you are
asking, you have failed, and the issue will sit there for days.

Assume the reader:

- has not looked at this issue for two weeks
- has not seen your branch and will not open it
- does not know the internal names of things
- has about ninety seconds

---

## The shape of the question

Post it as a comment on the issue. Use these five headings, in this order,
every time.

```markdown
### What I am trying to do

[One or two sentences, in the language of the product, not the code. What
should be true for a user when this is finished.]

### What I have done so far

[Two to five short bullets. Where you looked, what you found, what you have
already built or changed. If you pushed a branch, link it here.]

### Where I am stuck

[The one thing you cannot decide or cannot find out. Be specific. Say why it
is not something you can work out from the code by yourself.]

### The options as I see them

**Option 1: [short name]**
What it means for users: ...
Trade off: ...

**Option 2: [short name]**
What it means for users: ...
Trade off: ...

**What I would pick:** Option N, because [one sentence].

### What I need from you

[Exactly what to reply. Make it something they can answer in one line.]

Reply here with "option 1" or "option 2" and I will carry on from where I
stopped. If you would rather do it differently, describe it in your own words
and I will follow that instead. I will not touch this issue again until you
reply.
```

---

## Rules

1. **Ask one question.** If you have three, ask the one that blocks everything
   else. The others may answer themselves once you can move again. If two
   questions genuinely have to be answered together, put both in, and no more.

2. **Never ask something you can find out.** Read the code, read
   `Requirements.md`, read `docs/task-list.md`, read the git history, run the
   tests. Asking a person something that is written down in the repository
   wastes their time and yours.

3. **Always give options, and always recommend one.** "How should I do this?"
   puts all the work back on the reader. "Here are two ways, I would pick the
   first, is that alright?" can be answered while making coffee. If you can
   genuinely only see one way, present it as "I plan to do X, tell me if that
   is wrong" and give them the chance to stop you.

4. **Say what it means for users, not what it means in the code.** Not "should
   `getDocuments()` return an empty list or throw?" but "when a student has not
   uploaded anything yet, should the page show an empty list, or a message
   saying there is nothing here yet?".

5. **Say what happens if they ignore you.** Usually: nothing, the issue waits.
   Say that plainly. Do not create false urgency, and do not go ahead anyway.

6. **Do not lose your work.** If you have written useful code, push the branch
   before you ask and link to it. Say clearly that it is unfinished and has no
   pull request yet.

7. **Plain words.** Explain any technical term the first time you use it, in
   half a sentence. If a sentence would puzzle someone who is not a developer,
   rewrite it.

8. **Never ask for a credential, a password, an API key, or access to
   anything.** If the work needs one, say which one is missing and let a person
   decide what to do. Then label the issue `agent:blocked` and stop.

---

## A good example

> ### What I am trying to do
>
> Make documents and building photos actually open when someone clicks them.
> At the moment the browser gets a link it cannot reach, so nothing loads.
>
> ### What I have done so far
>
> - Found the cause: the links are built from the address the server uses to
>   talk to the file store (`minio:9000`). That address only works inside the
>   server. A browser on someone's laptop cannot reach it.
> - The fix is to sign the links with a public address instead. I have that
>   working locally on the branch `fix/issue-268-presigned-url-host` (pushed,
>   no pull request yet).
> - I checked how the deployment is set up in `docker-compose.yml` and there is
>   no public address for the file store configured anywhere yet.
>
> ### Where I am stuck
>
> Something has to give the browser a way to reach the file store, and that is
> a deployment decision rather than a code one. I cannot tell from the
> repository which of these you already have available.
>
> ### The options as I see them
>
> **Option 1: put the file store behind the existing web address**
> What it means for users: links work everywhere, nothing new to run.
> Trade off: needs a small routing rule added to the deployment, so someone
> with access to the server has to make that change once.
>
> **Option 2: give the file store its own address**
> What it means for users: same result.
> Trade off: another address to set up and keep secure, and it has to be
> configured for every environment separately.
>
> **What I would pick:** Option 1, because it uses what is already there and
> there is nothing extra to maintain.
>
> ### What I need from you
>
> Reply with "option 1" or "option 2". If you pick option 1, I will add the
> configuration and the code change together, and write down the one line
> someone needs to add to the deployment. Nothing happens on this issue until
> you reply.

## A bad example, and why

> The presigned URL host resolution is ambiguous. Should I inject
> `MINIO_PUBLIC_ENDPOINT` into `MinioFileStorageService` or handle it in the
> reverse proxy layer? Let me know your preference.

Wrong because: it never says what is broken for users, it assumes the reader
knows what a presigned URL is and what the file storage service does, it offers
no recommendation, it does not say what happens next, and it cannot be answered
without opening the code.

---

## Before you post, check

- [ ] Would someone who has never seen this code understand what is broken?
- [ ] Is there exactly one question?
- [ ] Have I given options and said which I would choose?
- [ ] Have I said what happens if they do not reply?
- [ ] Have I linked my branch if I have unfinished work?
- [ ] Have I removed every technical term I did not explain?
- [ ] Have I added the `agent:needs-input` label and removed `agent:working`?
