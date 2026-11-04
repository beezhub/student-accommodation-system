# ADR-009: Where CI Runs

- **Status:** Accepted
- **Date:** Decided 2026-11-02 (#330); written 2026-11-04
- **Task:** #287, split out of #284
- **Implemented by:** #337 (image builds), #285, #385 and #376 (less waste)
- **Deciders:** Boitumelo Tshehla

---

## Context

In September 2026 this repository was using most of its included GitHub Actions allowance in about
a week (#284). #284 asked to move CI onto a server we control, using open source tooling, so that
build minutes stop being a metered cost. #287 was split out to choose the platform.

Two things have changed since then:

1. **Most of the waste is gone.** #285 removed the double build on every merge and added
   concurrency control and path filters. #385 dropped CI on `main`, and #376 skipped the image build
   on release pull requests.
2. **Images need arm64 runners.** PROD (EC2 Graviton) and NONPROD (Oracle Ampere) are both arm64.
   GitHub now offers hosted arm64 runners (`ubuntu-24.04-arm`), so images build natively, without
   QEMU (an emulator that is slow for builds like these) and without a server of our own.

### What Actions costs today

GitHub's plan for this organization includes **$12 of Actions usage a month** on private
repositories. A Linux x64 minute deducts $0.006 from it, and a Linux arm64 minute deducts $0.005
([GitHub's billing docs](https://docs.github.com/en/billing/concepts/product-billing/github-actions)).
Public repositories do not use it.

Measured from `GET /orgs/beezhub/settings/billing/usage`, this repository only:

| Period | Linux x64 minutes | Linux arm64 minutes | Value at list price | Merged pull requests |
|---|---|---|---|---|
| September 2026 | 2,000 | 0 | $12.00 | 85 |
| October 2026 | 143 | 0 | $0.86 | 12 |
| 1 to 4 November 2026 | 704 | 231 | $5.38 | 49 |

September stopped at exactly the allowance. Every run from 13 September to the end of the month is
`skipped`. The organization's Actions budget is $0 with "prevent further usage" on.

November's rate is about **$0.11 per merged pull request**. It is on the high side: it includes
cold image builds, the first releases from `main`, and runs from before #385 and #376 landed on
3 November.

---

## Decision Drivers

| Driver | Weight |
|--------|--------|
| No server to patch, secure or keep running: one person maintains this project | High |
| Builds arm64 images natively | High |
| Low monthly cost: PROD's target is $30 a month and it is already about $35 (`docs/PROD_ENVIRONMENT_PLAN.md`) | High |
| Secrets stay away from the servers that run the application | High |
| Small change to the existing workflows | Medium |
| Open source tooling | Low |

---

## Options Considered

### Option A: GitHub-hosted runners (chosen)

`ubuntu-24.04-arm` for image builds and `ubuntu-latest` for everything else.

| Constraint from #287 | How it is met |
|---|---|
| Docker in the build | Hosted runners have Docker, and the Testcontainers suite runs on them in CI |
| Architecture | `images.yml` builds natively on arm64 |
| Secrets | ECR is reached through GitHub's OIDC token (a short-lived identity, so no stored AWS key). The workflows reference only two secrets: `JWT_SECRET` and `SSH_PRIVATE_KEY` |
| Pull request status checks | Built in. They show on the pull request but cannot block a merge, because branch protection is not available on this plan (#329) |
| Caching | Gradle and npm caches and the Docker layer cache use GitHub's cache. #377 is checking that the layer cache is warm |

**Cost:** see [Monthly cost](#monthly-cost).

**Cons:** usage stays metered, the budget can stop every workflow (see
[Consequences](#consequences)), and it depends on GitHub being up.

### Option B: Self-hosted runner

Workflow files barely change, and GitHub charges nothing for these minutes today. It announced a
fee of $0.002 a minute in December 2025 and then postponed it, so that could still change.

- **On the NONPROD server (free):** rejected. Jobs would run on the host that holds NONPROD's `.env`
  and an ECR login, and builds would compete with the application for CPU and memory. That would
  also make the load test in #334 harder to trust.
- **On a separate server:** rejected on cost and upkeep. PROD's `t4g.small` is $15.84 a month on
  demand, and 2 GB is unlikely to be enough for Gradle plus the four Testcontainers services, so a
  usable runner would cost more. Someone would also have to patch and watch it.

### Option C: Woodpecker, Forgejo Actions or Jenkins

All are open source, and all need the same server as the separate option in B, so they cost at least
as much. Each adds more work on top:

- Woodpecker and Jenkins need the pipelines rewritten.
- Forgejo needs its own instance mirroring the repository.
- Jenkins also needs ongoing plugin upkeep.
- All three would lose the GitHub OIDC trust that `infra/terraform/prod/iam.tf` relies on.

### Not considered further

Drone is not OSI open source since 2.x. Tekton and Argo Workflows assume Kubernetes, which this
project does not run.

---

## Decision

**Run all CI on GitHub-hosted runners.** Do not move to an open source CI platform.

This departs from #284's original goal on purpose. That goal was to stop metered minutes from being
a problem. With the waste removed, the included allowance covers normal work, so a server would cost
more money and more upkeep than the problem it solves.

No migration follows from this decision. The images already build on `ubuntu-24.04-arm` (#337), so
the follow-up issue #287 expected is not needed.

### Monthly cost

| Merged pull requests a month | Value at list price | Paid |
|---|---|---|
| 40, the estimate used in #330 | about $4.40 | $0 |
| About 100 | about $11 | $0 |
| About 365, November's pace held all month | about $40 | about $28, if the budget allows it |

Figures are before any VAT. The estimate of about $2.40 a month in #330 covered image builds only
and did not count the included $12, so this table replaces it.

### Revisit this decision when any of these happens

1. Paid Actions usage goes above $15 a month for two months in a row, which is about the price of a
   small server.
2. GitHub changes the included allowance or the runner prices enough to break the table above, or
   starts charging for self-hosted runners.
3. A build needs something a hosted runner cannot reach, such as a private network.

---

## Consequences

- **Positive:** there is no CI server to host, patch or secure.
- **Positive:** workflow files stay GitHub Actions, and the OIDC roles keep working unchanged.
- **Positive:** both servers run images built natively for their architecture, and PROD promotes
  the same bytes NONPROD ran.
- **Negative:** a busy month can use up the allowance. With the budget at $0, Actions then stops
  until the next month, including CI, Deploy NONPROD and Deploy PROD. `docs/PROD_RUNBOOK.md`,
  "By hand (when GitHub Actions is unavailable)", is the way to release to PROD in that case.
- **Negative:** CI, image builds and deploys all depend on GitHub being available.
- **Neutral:** the tooling is not open source, which was part of #284's request.
- **Neutral:** whether to keep the budget at $0 is a separate choice. Raising it swaps the hard stop
  for a small bill.
