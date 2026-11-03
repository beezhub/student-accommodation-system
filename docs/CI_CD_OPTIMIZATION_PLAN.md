# CI/CD optimization plan

How to make releases faster and steadier, from measurements of the pipeline as it stood on
2026-11-03, right after the first release from `main` (#372). Tracking issue: #373.

## The pipeline today

```
feature PR ──> develop ──> release PR ──> main ──> Deploy NONPROD ──> Deploy PROD (by hand)
   ci.yml                   ci.yml                  images.yml           deploy-prod.yml
   images.yml (build only)  images.yml              + deploy.yml         (#338)
```

- `ci.yml` builds and tests the backend and frontend on every pull request and push.
- `images.yml` builds both images for arm64. On a pull request it only builds; called from
  `deploy.yml` it also pushes to ECR.
- `deploy.yml` deploys NONPROD on every push to `main` and saves the release for PROD.
- `deploy-prod.yml` promotes a green NONPROD run to PROD.

## What was measured

| Step | Usual | Worst | Run |
|---|---|---|---|
| `ci.yml`, whole run | 3 min | 9 min | 41678334812 |
| Backend image build | 2 min | 20 min | 41683420912 (release PR into `main`) |
| Frontend image build | 1 min | 2 min | 41683420912 |
| NONPROD "Pull and start" | 1 min | 7 min | 41665456863 |
| NONPROD "Pull and start", failed | | 4.5 min | 41676623853, 41686766359 |

The backend image in ECR, layer by layer:

| Layer | Size | From `backend/Dockerfile` |
|---|---|---|
| Java runtime base | 70.1 MB | `eclipse-temurin:21-jre-alpine` |
| The jar | 95.6 MB | `COPY --from=build ... app.jar` |
| The jar again | 95.6 MB | `RUN chown spring:spring app.jar` |
| Total | 261.4 MB | |

## What the numbers say

1. **The deploy downloads about 191 MB every time.** The jar is stored twice, because `RUN chown`
   on a file from an earlier layer writes a full copy of it. And it is one layer, so any code
   change replaces all of it. The 70 MB base stays cached on the host.
2. **Long pulls drop the SSH connection.** Both failed NONPROD deploys lost the connection about
   4.5 minutes into the backend pull. `docker compose pull --quiet` prints nothing while it
   works, so the session is idle for minutes. An idle timeout on the network path is the likely
   cause. Since Deploy PROD only promotes a green NONPROD run, this blocks releases.
3. **The release PR builds images nobody uses.** Every change in it already passed the image
   build on its own PR, and the push to `main` builds them again for NONPROD. Being the first build
   aimed at `main`, it also started with no cache: 20 minutes.
4. **CI itself is fine.** Three minutes is not worth optimizing yet.

## Steps

In order. Step 1 unblocks releases; step 2 is the biggest saving.

| Step | Issue | Change | Expected effect |
|---|---|---|---|
| 1 | #375 | NONPROD deploy: SSH keep-alive, and retry the pull three times before `up`, as `tools/prod/start.sh` does | No failed deploys from a dropped connection; a failed pull never touches the running stack |
| 2 | #374 | Backend image: `COPY --chown` instead of `RUN chown`, and Spring Boot's layered jar | Image from 261 MB to about 166 MB; a code-only deploy downloads a few MB instead of 191 MB |
| 3 | #376 | `images.yml`: skip pull requests from `develop`, as `ci.yml` already does (GH-284) | No 20-minute build on each release PR |
| 4 | #377 | Check the Docker layer cache is warm on `main` and on branches, after step 2 | Confirms builds stay at about 2 minutes; if not, move the cache to ECR |

Steps 1 and 3 are small workflow changes. Step 2 changes the Dockerfile, so its first build starts
cold, which is why step 4 waits for it.

## How PROD benefits

PROD pulls the same images through `tools/prod/start.sh`. Step 2 makes its pull a few MB as well,
which shortens the restart gap of `up -d` (#246) and lowers ECR storage, which also helps the
lifecycle rule in #355.

## Not in this plan

- **Zero-downtime deploys.** `up -d` replaces containers in place. That is #246.
- **Faster tests.** CI is three minutes; revisit if it passes ten.
- **Building the jar in CI and copying it into the image.** It would save the second compile, but
  the image would then depend on the CI runner's build rather than its own Dockerfile.
