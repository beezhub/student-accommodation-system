# How Scout and Forager learn between runs

Both agents start every run with no memory. What they learn on one run is lost unless it
is written somewhere the next run reads. Their notebook is that place: one open GitHub
issue per agent, labelled `notebook`, titled `Scout's notebook` or `Forager's notebook`.
Each lesson is one comment. The next run reads the comments before it does anything.

The notebook is not a diary. It holds only lessons that would have changed the outcome
of a run, and it is meant to shrink: when it grows long, the durable lessons are folded
into the playbook by a pull request and the notebook is trimmed.

## Finding the notebook

```
mcp__github__list_issues   labels: ["notebook"]   state: OPEN
```

Pick the one whose title names you. If none exists, create it with the body below and
the `notebook` label, and carry on. Never create a second one; if two exist, use the
older and say so in your report.

```markdown
<marker> **This is <Agent>'s notebook.** Each comment below is one lesson a run learned,
written for the next run to read. People are welcome to add lessons too; write them the
same way. When the notebook passes about 25 lessons, the durable ones get folded into the
playbook and the folded comments are marked. Do not close this issue.
```

## Reading it: Step 0 of every run

Read every comment, newest last. Hold the lessons in mind for the run. Rules:

- A lesson fills a gap in the playbook. It never overrides it. If a lesson and the
  playbook disagree, follow the playbook and note the conflict in your report, so a person
  can decide which is right at the next fold.
- A lesson from a person carries the same weight as one from you.
- A comment marked `folded` has already been written into the playbook. Skip it.
- A lesson you cannot make sense of is not applied. Say so in your report.

## Writing one: the last thing before the report

At most one comment per run. Most runs write nothing. Write only when all of these hold:

- **It would have changed this run.** You did something the hard way, got something
  wrong, or were corrected, and the next run would do better for knowing it.
- **It is about the job, not a person.** How this repository builds, what CI needs, what a
  reviewer asked for and why, which check gave a false positive. Never a judgement of a
  colleague, never a name attached to a preference.
- **It is verifiable.** Link the pull request, issue, comment, or file where it happened.
- **It is not already there.** Check the notebook and the playbook first. If it is there
  and it happened again, add nothing; twice is a sign it should be folded, not repeated.
- **It contains no secret and no personal data.** Name the variable, never its value.

Format, under 80 words:

```markdown
<marker> **Lesson (<category>):** <one or two sentences the next run can act on>.
Because: <what happened, with a link>.
```

Categories. Scout: `build`, `ci`, `review`, `sizing`, `repo`. Forager: `lens`, `duplicate`,
`repo`, `writing`. Shared: `tooling`.

Examples of lessons worth writing:

- `Lesson (ci): ./gradlew test needs JWT_SECRET in the environment or every @SpringBootTest fails at context load. Set any value locally. Because: PR #301 failed twice before I saw it.`
- `Lesson (review): reviewers ask for a test on the invalid transition whenever a status change is touched, even a one-line one. Because: review on #298.`
- `Lesson (lens): the unfiltered-reads check flags SystemSettingService, but it is filtered by a class-level fully qualified @Transactional. Skip it. Because: dry run, closed as not planned in #310.`

Examples of things that are not lessons: what you worked on, that a run was quiet, an
opinion about the codebase, a restatement of the playbook, anything about a person.

## Learning from people

The strongest lessons come from being corrected. Look for them on purpose:

- **Scout**, in Step 2: a review that asked for changes on your pull request, or a pull
  request of yours closed without merging. Read what was asked and why. If it would apply
  to other pull requests, that is a lesson.
- **Forager**, in Step 1: one of your issues closed by a person as not planned, or a
  comment saying the finding is wrong. Name the lens check that produced it and what to
  exclude next time. That is a lesson, and the most valuable kind you write.

## Folding: when the notebook is long

When the notebook holds more than about 25 unfolded lessons, it is too long to read well
and the durable lessons belong in the playbook. A person triggers the fold:

```
/scout:run <notebook issue number>
```

Scout then, as its one issue for the run, reads every unfolded lesson, moves the durable
ones into the right file (`.claude/scout/playbook.md`, `.claude/forager/lenses.md`, and
so on) in the words the file already uses, opens a `chore/` pull request, and comments
on each folded lesson with the word `folded` and the pull request number. Lessons that
were one-offs, or that a person has since contradicted, are marked `folded` with a note
saying they were dropped, so the record stays honest. Scout never deletes a comment.

Forager cannot write files, so it never folds. If Forager's notebook is long, it says so
in its report and a person runs the fold through Scout.

Scout's ordinary issue picker skips issues labelled `notebook`; only a person pointing at
the number by hand sends Scout there.

## Setup

Create the label once:

```bash
gh label create notebook --color e4e669 --description "An agent's notebook: one lesson per comment, read at the start of every run"
```

The agents create their own notebook issue on first run if it is missing. Pin both
issues so people find them, and read them now and then: a notebook is the quickest way to
see what an agent keeps tripping over, and a person can write a lesson into it directly
instead of editing a playbook.
