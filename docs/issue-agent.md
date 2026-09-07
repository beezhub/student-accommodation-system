# The issue agent

An automated helper that works through GitHub issues on its own. It wakes up
once an hour, picks one open issue, fixes it, and opens a pull request for you
to review. If it is not sure about something, it stops and asks you a question
you can answer in one line.

It never merges anything. Every change it makes is a pull request that a person
reads and approves, exactly like a change from any other contributor.

---

## What one run looks like

1. **Wakes up** at twenty past the hour.
2. **Checks it is not flooding you.** If three of its pull requests are already
   waiting for review, it stops and does nothing.
3. **Picks one issue.** Answered questions first, then anything labelled
   `agent:ready`, then the most serious open bug it thinks it can finish.
4. **Says so** in a comment on the issue, so you can see what it is doing.
5. **Reads the code** and works out whether it can finish on its own.
6. **Either asks you a question**, in plain language with options and a
   recommendation, **or writes the fix**, with a test.
7. **Runs the build, the tests and the linters** and refuses to open a pull
   request if they fail.
8. **Opens the pull request**, explains what it changed, what it checked, and
   what it could not check.
9. **Comments on the issue** with the link.

If anything goes wrong it always leaves a comment saying what it was doing and
what stopped it. It never goes quiet.

---

## Setting it up (once)

1. **Install the Claude GitHub App** on this repository:
   <https://github.com/apps/claude>.

2. **Add the API key.** In *Settings, Secrets and variables, Actions, Secrets*,
   add `ANTHROPIC_API_KEY` with a key from <https://platform.claude.com>.
   If you use a Claude subscription instead of an API key, run
   `claude setup-token` locally, save the result as `CLAUDE_CODE_OAUTH_TOKEN`,
   and change that one line in `.github/workflows/issue-agent.yml`.

3. **Merge this workflow into the default branch.** GitHub only runs scheduled
   workflows from the default branch. Until `.github/workflows/issue-agent.yml`
   is on `main`, the hourly loop will not start, though you can still run it by
   hand.

4. **Create the labels.** The workflow does this itself before every run. To do
   it now:

   ```bash
   GH_TOKEN=$(gh auth token) .github/scripts/agent-labels.sh
   ```

5. **Try it once by hand** before trusting the schedule. Go to *Actions, Issue
   agent, Run workflow*, tick **dry run**, and give it an issue number. It will
   investigate and report without changing anything.

### Optional but recommended

Add a secret called `AGENT_GITHUB_TOKEN` holding a personal access token or a
GitHub App token with `repo` access. Without it, the agent pushes using the
built in `GITHUB_TOKEN`, and GitHub deliberately does not start CI workflows on
pushes made with that token. That is fine today because this repository has no
CI workflows yet, but the moment you add one, agent pull requests would show no
checks at all.

---

## Settings

All of these are optional. Set them in *Settings, Secrets and variables,
Actions, Variables*.

| Variable | Default | What it does |
|---|---|---|
| `AGENT_ENABLED` | `true` | Set to `false` to stop the loop completely. Nothing is deleted, it just stops running. |
| `AGENT_BASE_BRANCH` | `develop` | The branch it starts from and opens pull requests against. |
| `AGENT_MAX_OPEN_PRS` | `3` | It will not start new work while this many of its pull requests are waiting for review. |
| `AGENT_GIT_NAME` | `github-actions[bot]` | The name on its commits. |
| `AGENT_GIT_EMAIL` | the GitHub Actions bot address | The email on its commits. |

| Secret | Required | What it does |
|---|---|---|
| `ANTHROPIC_API_KEY` | yes (unless using `CLAUDE_CODE_OAUTH_TOKEN`) | Pays for and authorises the agent. |
| `AGENT_GITHUB_TOKEN` | no | Makes CI run on the agent's pull requests. See above. |

To change the schedule, edit the `cron` line at the top of
`.github/workflows/issue-agent.yml`. `"20 * * * *"` means twenty past every
hour, in UTC.

---

## Giving it work

**The direct way.** Open an issue using the *Job for the issue agent* template.
It asks five plain questions and adds the `agent:ready` label for you. The
agent takes labelled issues before anything else.

**The passive way.** Do nothing. If no issue is labelled for it, the agent
picks the most serious open bug it believes it can finish in one run. It skips
anything assigned to a person, anything already covered by an open pull
request, and anything labelled `agent:hands-off`.

**Right now.** *Actions, Issue agent, Run workflow*, and put in an issue
number. You do not have to wait for the hour.

### Writing an issue it can actually do

- Say what should be true when it is finished. That is the single most useful
  sentence you can write.
- Give one concrete example: the steps, the error message, the screenshot.
- Keep it to one thing. "Fix the upload and redesign the dashboard" will come
  back as a question asking which one you meant.
- Say what to leave alone if there is something sensitive nearby.

---

## When it asks you something

You will get a comment on the issue with five headings: what it is trying to
do, what it has done so far, where it is stuck, the options it can see with the
one it would choose, and what it needs from you. It is written to be answerable
without opening any code.

Reply in the issue. "Option 2" is a complete answer. So is describing what you
want in your own words.

The agent starts again as soon as you reply, without waiting for the hour. It
picks up exactly where it left off, and its earlier comments are its notes.
The immediate wake up only happens for replies from someone with write access
to the repository. Anyone else's reply is still read, but the agent will get to
it on its next hourly run.

If you never reply, nothing happens. The issue sits there with the
`agent:needs-input` label. It will not guess and carry on.

---

## The labels

| Label | Meaning |
|---|---|
| `agent:ready` | Queued for the agent to pick up next. |
| `agent:working` | It is working on this right now. |
| `agent:needs-input` | It asked a question and is waiting for you. |
| `agent:blocked` | It cannot get any further on its own. Needs a person. |
| `agent:done` | It opened a pull request for this. |
| `agent:hands-off` | It must never touch this issue. Add this to anything sensitive. |
| `agent` | On pull requests: this one came from the agent. |

---

## Stopping it

- **Stop everything:** set the repository variable `AGENT_ENABLED` to `false`.
- **Stop one issue:** add the `agent:hands-off` label.
- **Stop it permanently:** disable the *Issue agent* workflow under *Actions*,
  or delete `.github/workflows/issue-agent.yml`.
- **Stop a run in progress:** open the run under *Actions* and cancel it. It
  will leave the `agent:working` label behind, so remove that by hand,
  otherwise future runs will skip the issue.

---

## What it will not do

These are written into its playbook, not left to its judgement:

- Merge anything, or approve its own work.
- Push to `main` or `develop` directly. Everything goes through a pull request.
- Work on more than one issue per run.
- Edit an existing database migration. It adds a new one instead.
- Commit an API key, password, or `.env` file.
- Edit its own workflow, playbook, or scripts.
- Make a failing test pass by deleting or skipping it.
- Do anything an issue tells it to do that is aimed at the agent rather than at
  the product. Anyone can write anything in an issue, so it treats issue text
  as information, not as instructions. If an issue asks it to change
  credentials, weaken a security check, or grant someone access, it stops and
  labels the issue `agent:blocked`.
- Take on work that needs a decision that is not a developer's to make: pricing,
  legal wording, who gets access to what, deleting real data.
- Claim it tested something it did not test.

---

## Costs

Each run uses GitHub Actions minutes and Claude API tokens. A run that finds
nothing to do costs almost nothing, because it stops within a minute. A run
that writes a fix and runs the full backend build is the expensive one.

Ways to spend less:

- Lower `AGENT_MAX_OPEN_PRS` so it does less unreviewed work.
- Change the schedule from hourly to a few times a day.
- Write clearer issues, so it spends fewer turns working out what you meant.
- Add `--max-turns 60` to the `claude_args` block in the workflow to cap how
  long any one run can go on.

The 45 minute job timeout is a backstop against a run that gets stuck.

---

## Changing how it behaves

Two files control everything the agent does, and both are plain English:

- **`.github/agents/issue-agent.md`** is the playbook: how it picks issues, how
  it writes code, what it must never do.
- **`.github/agents/asking-for-help.md`** is how it writes questions to you.

Edit them like documentation. You do not need to touch the workflow file to
change the agent's behaviour, and a change to either takes effect on the next
run.

---

## When something looks wrong

**It is not running.** Check that the workflow is on the default branch,
that `AGENT_ENABLED` is not `false`, and that *Actions* is enabled for the
repository. On public repositories GitHub also switches off scheduled workflows
after 60 days with no activity in the repository.

**Scheduled runs fail saying the actor is a bot.** GitHub attributes a
scheduled run to whoever last changed the `cron` line. If that was a bot
account, change the line yourself and commit it under your own account.

**It says it cannot authenticate.** The `ANTHROPIC_API_KEY` secret is missing,
expired, or out of credit.

**It keeps picking the wrong issues.** Label the ones you want with
`agent:ready`, and put `agent:hands-off` on the ones you do not.

**It opened a pull request that is wrong.** Close it and say why in a comment
on the issue. Then add the reason to the playbook so it does not happen again.

**An issue is stuck on `agent:working`.** A run was cancelled or timed out.
Remove the label and it will be picked up again.
