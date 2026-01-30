# QA & Test Automation Plan: Student Accommodation System

## 1. Test Strategy Summary
*   **Objective:** Ensure the reliability of the core "Application-to-Allocation" pipeline, verify strict Role-Based Access Control (RBAC), and guarantee data integrity for room inventory.
*   **Tech Stack:** 
    *   **Backend:** JUnit 5, Mockito, TestContainers.
    *   **Frontend:** Jasmine/Karma (Unit), Playwright (E2E).
*   **Key Risks:** Race conditions in room booking, PII data exposure, and document storage reliability.

---

## 2. Test Plan Overview

### 2.1 Testing Scope
| Component | Functionality | Test Level | Priority |
| :--- | :--- | :--- | :--- |
| **Authentication** | JWT Login, Registration, Route Guards | Unit, E2E | P0 |
| **Student Portal** | Application Form, Document Upload, Status Tracking | Unit, E2E | P0 |
| **Admin Portal** | Application Review, Verification, Manual Allocation | Unit, E2E | P0 |
| **Core Logic** | Allocation Scoring, Gender/Block Rules, Notifications | Unit | P1 |
| **Infrastructure** | Database Transactions, MinIO Storage | Integration | P1 |

### 2.2 Environment Requirements
*   **Database:** PostgreSQL (Clean state per test run).
*   **Object Storage:** MinIO (Mock or Test Bucket).
*   **CI Environment:** GitHub Actions with Headless Chrome.

---

## 3. Unit Testing Strategy

### 3.1 Backend (Java Spring Boot)
Tests should focus on the `Service` layer logic and `Repository` constraints.

**Example: Application Submission Logic**
```java
@Test
void submitApplication_ValidInput_Success() {
    // Verify application status changes to SUBMITTED
    // Verify email notification is triggered
}

@Test
void submitApplication_AlreadyExists_ThrowsException() {
    // Verify user cannot submit multiple active applications
}
```

### 3.2 Frontend (Angular)
Tests should focus on component state and service interactions.

**Example: Status Badge Color Logic**
```typescript
it('should display "Approved" badge in green', () => {
  component.status = 'APPROVED';
  fixture.detectChanges();
  const badge = fixture.nativeElement.querySelector('.status-badge');
  expect(badge.classList).toContain('status-green');
});
```

---

## 4. E2E Test Scenarios (Playwright)

### TC-E2E-01: Full Lifecycle (Happy Path)
*   **Description:** A student applies, an admin verifies, and a room is allocated.
*   **Preconditions:** Student user exists; Rooms are vacant.
*   **Steps:**
    1. Login as Student.
    2. Complete application form + Upload ID.
    3. Login as Admin.
    4. Approve Application & Verify Document.
    5. Navigate to Allocation Dashboard and assign Room 101.
*   **Expected Result:** Student status updates to "Allocated"; Room 101 status becomes "Occupied".

### TC-E2E-02: Security / RBAC
*   **Description:** Ensure students cannot access admin endpoints.
*   **Steps:**
    1. Login as Student.
    2. Manually navigate to `/admin/user-management`.
*   **Expected Result:** User is redirected to Dashboard or shown a 403 error.

---

## 5. Test Case Inventory & Priority

| ID | Suite | Description | Priority | Automation |
| :--- | :--- | :--- | :--- | :--- |
| **AUTH-01** | Auth | Login with valid credentials | P0 | Playwright |
| **APP-01** | Student | Submit application with missing mandatory fields | P0 | Jasmine |
| **DOC-01** | Admin | Download and view uploaded student documents | P1 | Playwright |
| **ALC-01** | Core | Prevent allocation of Male student to Female block | P0 | JUnit |
| **STAT-01** | Dashboard | Verify "Vacant Rooms" count updates in real-time | P2 | Playwright |

---

## 6. Coverage Matrix & Goals
*   **Backend Service Layer:** > 90% Line Coverage.
*   **Core Allocation Logic:** 100% Branch Coverage.
*   **Critical User Journeys:** 100% Automated (Playwright).
*   **Mobile Responsiveness:** Manual verification on Tablet/Mobile break-points.
