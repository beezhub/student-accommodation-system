# MVP Execution Plan (Backend-First)

Date: 2026-01-03  
Owner: Team (Backend, Frontend, DevOps)

## Goals
- Ship document verification + automatic room allocation per `BACKEND_REQUIREMENTS.md`.
- Add application preference fields end-to-end with data backfill.
- Complete admin/room UIs wired to live APIs and notifications.
- Establish tests, CI, and production-ready Docker/compose.
- Enable admin management for buildings, institutions, and documents (CRUD + search).
- Introduce automated waiting list when no rooms are available.

## Scope & Sequencing
1) Backend first to unblock UI.
2) Frontend admin/room flows once APIs stable.
3) Quality gates (tests/CI) then deployment packaging.

## Work Breakdown
- **B1: Document Status & Room Allocation** (Backend) **[Done]**
  - Implement transactional status update, required-doc check, room allocation, enriched response. ✅
  - Add applicationStatus filter to document search. ✅
  - Logging, security annotations, optimistic locking as per requirements. ✅ (`@Version` on Application/Room/Document; domain exceptions wired)
  - Add waiting-list enqueue when no rooms available, with notification hook and dequeue logic on room free. ✅ (enqueue on doc approval; dequeue on allocation end/delete)
- **B2: Preference Fields E2E** (Backend)
  - Add preference fields to entity/DTO/mapper/repo/service/controller.
  - Database migration + backfill script.
  - Integration tests for persistence.
- **B3: Admin Entities Management** (Backend)
  - CRUD + search endpoints for buildings, institutions, and document types/status configs.
  - Add validation, pagination/filtering specs, and role-based access control.
  - Seed/migration updates for default entities as needed.
- **F1: Admin Review/Approval/Allocation UI** (Frontend)
  - Review queue, document approval, allocation actions, status/notification display.
  - Wire to live APIs; show room assignment.
- **F2: Room/Building Management UI** (Frontend)
  - Room/building CRUD; student view of assigned room.
  - Admin screens for institutions and document management; table filters/sorts and bulk actions.
  - Waiting-list visibility and manual override (assign, reorder, notify).
- **Q1: Tests & CI** (Backend/Frontend/DevOps)
  - Backend unit/integration for doc/room/preference/backfill.
  - Add tests for waiting-list enqueue/dequeue and notification flow.
  - Frontend e2e smoke (admin + student critical paths).
  - GitHub Actions CI: lint/test/build + coverage.
- **D1: Deployables** (DevOps)
  - Frontend multi-stage Dockerfile; `docker-compose.prod.yml` with envs, health checks.
  - ENV docs and runbook.

## Data Backfill
- Add script/migration to populate new preference fields for existing applications before go-live.
- Schedule backfill run pre-deploy; verify with integration test snapshot.

## MCP/Codegen Leverage
- Generate DTOs/mappers (MapStruct) for new preference fields.
- Scaffold migration + backfill SQL templates.
- Generate Angular services/components for admin review/room management flows.
- Template GitHub Actions CI and Docker/compose snippets.
- Generate CRUD scaffolds for building/institution/document management (backend + Angular tables/forms) from OpenAPI.

## Checkpoints
- Backend APIs validated via Postman/Swagger (doc update + allocation).
- Preference data persists and backfill verified.
- Admin → student room visibility demoed end-to-end.
- Admin CRUD for buildings/institutions/documents demoed with validation and search.
- Waiting list auto-enqueue/dequeue demoed when rooms are full and when room frees up.
- CI pipeline green with coverage; images build successfully.
- Stack boots via compose with health checks passing.

## Risks & Mitigations
- Room availability edge cases → add tests for no-available-room and concurrent approvals; ensure waiting-list ordering fairness.
- Backfill accuracy → dry-run and audit logs before prod run.
- API/UI drift → lock contracts before UI wiring; use shared typings via codegen.
- Admin CRUD consistency → enforce RBAC and optimistic locking; add audit logs for changes.

## Next Actions
- **Backend:** Start B2 preference fields (schema + backfill + tests). Add backend tests for waiting-list enqueue/dequeue and doc-required checks.
- **Infra/Build:** Ensure builds run on JDK 17+; run Flyway migrations (`waiting_list_entry`, version columns). Hook CI to enforce.
- **Frontend:** Begin F1 with live APIs for doc review/allocation once backend is stable; expose waiting-list visibility.
- **Ops:** Prepare CI/Docker templates; add health checks to compose.
