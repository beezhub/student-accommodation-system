## ADDED Requirements

### Requirement: CI pipeline runs on every PR
The system SHALL run a CI pipeline on every pull request targeting `main` or `develop`. The pipeline MUST build the backend JAR, run all backend unit tests, run frontend lint, and run frontend unit tests. A PR MUST NOT be mergeable if the CI pipeline fails.

#### Scenario: Backend build passes
- **WHEN** a pull request is opened or updated
- **THEN** the pipeline runs `./gradlew build` in the `backend/` directory and reports success

#### Scenario: Backend tests pass
- **WHEN** a pull request is opened or updated
- **THEN** the pipeline runs `./gradlew test` and all tests pass

#### Scenario: Frontend lint passes
- **WHEN** a pull request is opened or updated
- **THEN** the pipeline runs `npm run lint` in the `frontend/` directory and reports no errors

#### Scenario: Frontend tests pass
- **WHEN** a pull request is opened or updated
- **THEN** the pipeline runs `npm test -- --watch=false --browsers=ChromeHeadless` and all tests pass

### Requirement: Secrets scanning runs on every push
The system SHALL run a secrets scanner (`gitleaks`) on every push to any branch. The pipeline MUST fail and block merging if any credential, API key, or secret pattern is detected in the diff.

#### Scenario: No secrets detected
- **WHEN** a push contains no credential patterns
- **THEN** the secrets scan step passes and the pipeline continues

#### Scenario: Secret detected in diff
- **WHEN** a push contains a line matching a credential pattern (API key, JWT secret, password)
- **THEN** the secrets scan step fails, the pipeline is marked failed, and the PR is blocked

### Requirement: Docker images are built and tagged on merge to main
The system SHALL build Docker images for the backend and frontend on every push to `main`. Images MUST be tagged with both the git SHA and a semantic version tag derived from the git tag (if present). Images MUST be pushed to GitHub Container Registry (GHCR).

#### Scenario: Image built and pushed on main merge
- **WHEN** a commit is merged to `main`
- **THEN** the pipeline builds `backend/Dockerfile` and `frontend/Dockerfile`, tags them with the short git SHA, and pushes to `ghcr.io/<org>/<repo>-backend` and `ghcr.io/<org>/<repo>-frontend`

#### Scenario: Image tagged with semantic version on git tag
- **WHEN** a git tag matching `v*.*.*` is pushed
- **THEN** the pipeline additionally tags the images with the semantic version string

### Requirement: Pipeline uses GitHub Secrets for credentials
The CI pipeline MUST NOT hardcode any credentials. All secrets (GHCR token, any signing keys) SHALL be stored as GitHub Actions repository secrets and referenced via `${{ secrets.NAME }}`.

#### Scenario: No credentials in workflow files
- **WHEN** the workflow YAML files are read
- **THEN** no API keys, passwords, or tokens appear as literal values; all sensitive values reference `${{ secrets.* }}`
