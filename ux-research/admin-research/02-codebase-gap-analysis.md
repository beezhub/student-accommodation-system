# Codebase Gap Analysis & Information Architecture

## Current State: What BeezhHub Admin Has Today

### Strengths (Solid Foundation)
The current platform has a well-architected application-to-allocation lifecycle:

| Domain | Current Capabilities | Maturity |
|---|---|---|
| **Applications** | Submit, review, status workflow (10 states), request changes, save review progress, move to allocation | Production-ready |
| **Allocations** | Manual + auto-allocation, batch propose/confirm, staged review, scoring algorithm, waiting list, offer expiry, check-in/out | Production-ready |
| **Buildings/Rooms** | CRUD, bulk room creation, image management, attributes (gender, accessibility, desirability), geocoding | Production-ready |
| **Documents** | Upload/download (MinIO), verification workflow, comments, document types per tenant | Production-ready |
| **Multi-tenancy** | Hibernate tenant filter, tenant context, platform vs tenant admin roles, system settings cascade | Production-ready |
| **Notifications** | Email with async send, retry logic, SSE real-time push, templates | Production-ready |
| **Audit** | Activity logs with filters, CSV export | Basic |
| **Dashboard** | App counts by status, doc counts, room counts, occupancy stats, charts | Basic |

### Dormant / Partially Built (Code Exists, Not Wired Up)

| Entity/Feature | Status | Gap |
|---|---|---|
| `IntakePeriod` | Entity exists, no controller/UI | Could define application windows — needs REST + frontend |
| `CustomApplicationField` / `CustomFieldAnswer` | Entities exist, no controller/UI | Tenant-configurable forms — partially designed |
| `AllocationRule` | Entity + service exist, **no controller** | Admins can't manage scoring rules via UI |
| `BUILDING_STAFF` role | In `UserRole` enum | No guards, controllers, or UI support |
| `ApplicationBuildingPreference` | Entity used internally | No dedicated management UI |

### Critical Gaps (Nothing Exists)

| Gap | Priority | Justification |
|---|---|---|
| **Financial Module** | P0 | No rent/billing, invoicing, payment tracking, arrears, or NSFAS reconciliation. This is the #1 reason owners would NOT pay for the platform. |
| **Lease Management** | P0 | Allocations exist but no formal lease contracts, e-signatures, or lease lifecycle tracking. NSFAS requires electronic leases. |
| **NSFAS Integration** | P0 | No accreditation tracking, NSFAS-format invoice generation, payout reconciliation, or compliance document management. |
| **Maintenance/Work Orders** | P1 | No way to log, assign, track, or cost maintenance requests. Standard in every property management tool. |
| **Occupancy Dashboard** | P1 | Basic stats exist but no real-time heatmaps, historical trends, vacancy forecasting, or building-level breakdown. |
| **Reporting Engine** | P1 | Only activity log CSV export. No configurable financial, occupancy, or operational reports. |
| **Communication Center** | P2 | Notifications are system-generated only. No admin-to-student messaging, announcements, or broadcast. |
| **Room Inspection** | P2 | No move-in/move-out condition reports, damage assessment, or deposit deduction workflow. |
| **Inventory/Asset Tracking** | P2 | No furniture, appliance, or key management. |
| **Bulk Import/Export** | P2 | No CSV/Excel import for students, rooms, or buildings. |

---

## Proposed Information Architecture for Multi-Building Owners

### Current Navigation (5 groups, allocation-centric)
```
Overview > Dashboard
Applications > Applications | Allocations | Staged Allocations | Waiting List
Buildings & Rooms > Buildings | Rooms
Configuration > Accommodation Types | Institutions | Years of Study | Academic Years | Document Types | System Settings* | Feature Flags*
Administration > Users* | Tenants* | Documents | Activity Logs*
```

### Proposed Navigation (8 groups, operations-centric)

```
PORTFOLIO OVERVIEW
├── Dashboard (unified KPIs: occupancy, revenue, applications, maintenance)
├── Portfolio Analytics (cross-building comparison, trends)

STUDENT LIFECYCLE
├── Applications (existing)
├── Allocations (existing)  
├── Staged Allocations (existing)
├── Waiting List (existing)
├── Leases (NEW — active leases, renewals, expirations, e-signatures)

PROPERTY MANAGEMENT
├── Buildings (existing, enhanced with occupancy indicators)
├── Rooms (existing, enhanced with status: occupied/vacant/maintenance)
├── Maintenance (NEW — work orders, assignments, cost tracking)
├── Inspections (NEW — move-in/move-out, condition reports)

FINANCIALS
├── Invoicing (NEW — generate, send, track payment)
├── Rent Collection (NEW — payment tracking, arrears, receipts)
├── NSFAS Reconciliation (NEW — payout matching, discrepancy alerts)
├── Financial Reports (NEW — income statements, occupancy revenue, arrears aging)

DOCUMENTS
├── Document Verification (existing)
├── Compliance (NEW — NSFAS accreditation docs, B-BBEE, DHET norms)

COMMUNICATION
├── Announcements (NEW — building-wide or segment-targeted)
├── Notifications (existing, enhanced)

CONFIGURATION
├── Accommodation Types | Institutions | Academic Years | etc. (existing)
├── Allocation Rules (WIRE UP existing entity)
├── Intake Periods (WIRE UP existing entity)
├── System Settings | Feature Flags (existing)

ADMINISTRATION (Platform Admin only)
├── Users | Tenants | Activity Logs (existing)
```

### Key IA Principles for Multi-Building Owners

1. **Building switcher in the top bar** — owners managing 5+ buildings need instant context switching without navigating back to a list
2. **Portfolio-level default, building-level drill-down** — dashboard shows aggregate KPIs, click into a building for detail
3. **Role-based navigation pruning** — `BUILDING_STAFF` sees only their assigned building's Maintenance + Inspections; `TENANT_ADMIN` sees everything for their buildings; `PLATFORM_ADMIN` sees cross-tenant
4. **NSFAS vs private student toggle** — financial views need to separate NSFAS-funded students (invoiced to NSFAS) from private-paying students (invoiced directly)
