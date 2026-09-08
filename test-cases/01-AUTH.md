# Authentication & Account Management — UAT Test Cases

## Module Overview

This document contains exhaustive User Acceptance Test cases for the **Authentication & Account Management** module of the BeeZhub Student Accommodation System. The module covers user signup, login, email verification, password management (forgot, reset, change), logout/session handling, and account management (deletion, anonymization, lockout).

**Application URLs:**
- Frontend: `http://localhost:4200`
- Backend API: `http://localhost:8080`
- MailHog (email testing): `http://localhost:8025`

**Test Data:**

| Role | Email | Password | Name |
|------|-------|----------|------|
| Platform Admin | platformadmin@test.com | Admin@12345 | — |
| Tenant Admin | tenantadmin@test.com | Admin@12345 | — |
| Student A | john.doe@test.com | Student@123 | John Doe |
| Student B | jane.smith@test.com | Student@123 | Jane Smith |
| New User (signup) | newuser@test.com | NewUser@123 | New User |

---

## A. Signup (`/signup`)

---

### TC-AUTH-001: Successful signup with valid data

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-001 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | User is not logged in. Email `newuser@test.com` is not already registered. MailHog inbox is clear. |
| **Test Data** | firstName: `New`, lastName: `User`, email: `newuser@test.com`, password: `NewUser@123`, confirmPassword: `NewUser@123`, agreeTerms: checked |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/signup` | Signup page loads with brand panel (left) and form panel (right). Form title reads "Create your account". |
| 2 | Enter `New` in the First Name field | Field accepts input, no validation error. |
| 3 | Enter `User` in the Last Name field | Field accepts input, no validation error. |
| 4 | Enter `newuser@test.com` in the Email Address field | Field accepts input, no validation error. |
| 5 | Enter `NewUser@123` in the Password field | Field accepts input, no validation error. Password is masked. |
| 6 | Enter `NewUser@123` in the Confirm Password field | Field accepts input, no validation error. |
| 7 | Check the "I agree to the Terms of Service and Privacy Policy" checkbox | Checkbox is checked. |
| 8 | Click "Create Account" button | Button shows spinner with "Creating Account..." text. After success, a green snackbar appears: "Account created! Please check your email to verify your account before submitting applications." User is redirected to `/profile-setup?verificationPending=true`. |

| Field | Value |
|-------|-------|
| **Postconditions** | User account created in database. User redirected to profile setup. Verification email queued. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-002: Signup with empty firstName

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-002 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | User is on the signup page (`/signup`). |
| **Test Data** | firstName: (empty), lastName: `User`, email: `newuser@test.com`, password: `NewUser@123`, confirmPassword: `NewUser@123`, agreeTerms: checked |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/signup` | Signup page loads. |
| 2 | Leave First Name field empty | No error shown yet (not touched). |
| 3 | Fill in all other fields with valid data and check agreeTerms | Other fields valid. |
| 4 | Click into the First Name field and then tab out (blur) | Validation error appears under the field: **"First name is required"** |
| 5 | Observe the "Create Account" button | Button is disabled because the form is invalid. |
| 6 | Attempt to click "Create Account" | Nothing happens; button remains disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | No account created. User remains on signup page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-003: Signup with empty lastName

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-003 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | User is on the signup page (`/signup`). |
| **Test Data** | firstName: `New`, lastName: (empty), email: `newuser@test.com`, password: `NewUser@123`, confirmPassword: `NewUser@123`, agreeTerms: checked |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/signup` | Signup page loads. |
| 2 | Enter `New` in First Name | No error. |
| 3 | Leave Last Name field empty, fill in all other fields with valid data | Other fields valid. |
| 4 | Click into the Last Name field and then tab out (blur) | Validation error appears: **"Last name is required"** |
| 5 | Observe the "Create Account" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | No account created. User remains on signup page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-004: Signup with empty email

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-004 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | User is on the signup page (`/signup`). |
| **Test Data** | firstName: `New`, lastName: `User`, email: (empty), password: `NewUser@123`, confirmPassword: `NewUser@123`, agreeTerms: checked |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/signup` | Signup page loads. |
| 2 | Fill in all fields except Email. Leave Email field empty. | Other fields valid. |
| 3 | Click into the Email field and then tab out (blur) | Validation error appears: **"Email is required"** |
| 4 | Observe the "Create Account" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | No account created. User remains on signup page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-005: Signup with invalid email (no @ symbol)

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-005 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | User is on the signup page (`/signup`). |
| **Test Data** | firstName: `New`, lastName: `User`, email: `newusertest.com`, password: `NewUser@123`, confirmPassword: `NewUser@123`, agreeTerms: checked |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/signup` | Signup page loads. |
| 2 | Enter `newusertest.com` in the Email field | Input accepted. |
| 3 | Tab out of the Email field | Validation error appears: **"Please enter a valid email address"** |
| 4 | Observe the "Create Account" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | No account created. User remains on signup page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-006: Signup with invalid email (no domain)

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-006 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | User is on the signup page (`/signup`). |
| **Test Data** | firstName: `New`, lastName: `User`, email: `newuser@`, password: `NewUser@123`, confirmPassword: `NewUser@123`, agreeTerms: checked |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/signup` | Signup page loads. |
| 2 | Enter `newuser@` in the Email field | Input accepted. |
| 3 | Tab out of the Email field | Validation error appears: **"Please enter a valid email address"** |
| 4 | Observe the "Create Account" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | No account created. User remains on signup page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-007: Signup with empty password

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-007 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | User is on the signup page (`/signup`). |
| **Test Data** | firstName: `New`, lastName: `User`, email: `newuser@test.com`, password: (empty), confirmPassword: `NewUser@123`, agreeTerms: checked |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/signup` | Signup page loads. |
| 2 | Fill in all fields except Password. Leave Password field empty. | Other fields valid. |
| 3 | Click into the Password field and then tab out (blur) | Validation error appears: **"Password is required"** |
| 4 | Observe the "Create Account" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | No account created. User remains on signup page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-008: Signup with password less than 8 characters

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-008 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | User is on the signup page (`/signup`). |
| **Test Data** | firstName: `New`, lastName: `User`, email: `newuser@test.com`, password: `Short1!`, confirmPassword: `Short1!`, agreeTerms: checked |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/signup` | Signup page loads. |
| 2 | Enter `Short1!` (7 characters) in the Password field | Input accepted. |
| 3 | Tab out of the Password field | Validation error appears: **"Password must be at least 8 characters"** |
| 4 | Observe the "Create Account" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | No account created. User remains on signup page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-009: Signup with mismatched passwords

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-009 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | User is on the signup page (`/signup`). |
| **Test Data** | firstName: `New`, lastName: `User`, email: `newuser@test.com`, password: `NewUser@123`, confirmPassword: `DifferentPass1`, agreeTerms: checked |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/signup` | Signup page loads. |
| 2 | Enter `NewUser@123` in the Password field | No error. |
| 3 | Enter `DifferentPass1` in the Confirm Password field | Input accepted. |
| 4 | Tab out of the Confirm Password field | Validation error appears: **"Passwords do not match"** |
| 5 | Observe the "Create Account" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | No account created. User remains on signup page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-010: Signup without agreeing to terms

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-010 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | User is on the signup page (`/signup`). |
| **Test Data** | firstName: `New`, lastName: `User`, email: `newuser@test.com`, password: `NewUser@123`, confirmPassword: `NewUser@123`, agreeTerms: unchecked |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/signup` | Signup page loads. |
| 2 | Fill in all fields with valid data but leave the "I agree to the Terms of Service and Privacy Policy" checkbox unchecked | All text fields valid, checkbox unchecked. |
| 3 | Observe the "Create Account" button | Button is disabled because `agreeTerms` is `requiredTrue`. |
| 4 | Touch the checkbox (click to check, then uncheck) | Validation error appears: **"You must agree to the terms and conditions"** |

| Field | Value |
|-------|-------|
| **Postconditions** | No account created. User remains on signup page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-011: Signup with already-registered email

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-011 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | User `john.doe@test.com` already exists in the system. |
| **Test Data** | firstName: `John`, lastName: `Doe`, email: `john.doe@test.com`, password: `NewUser@123`, confirmPassword: `NewUser@123`, agreeTerms: checked |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/signup` | Signup page loads. |
| 2 | Enter `John` in First Name, `Doe` in Last Name | No error. |
| 3 | Enter `john.doe@test.com` in Email | No client-side error (uniqueness is server-side). |
| 4 | Enter `NewUser@123` in Password and Confirm Password | No error. |
| 5 | Check agreeTerms checkbox | Checked. |
| 6 | Click "Create Account" | Button shows spinner. API returns an error. Red snackbar appears with a message indicating the email is already registered (e.g., "Email is already in use" or similar backend message). User remains on signup page. |

| Field | Value |
|-------|-------|
| **Postconditions** | No duplicate account created. User remains on signup page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Verify exact error message from backend. |

---

### TC-AUTH-012: Signup triggers welcome/verification email in MailHog

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-012 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | MailHog inbox is cleared. `newuser@test.com` is not registered. |
| **Test Data** | firstName: `New`, lastName: `User`, email: `newuser@test.com`, password: `NewUser@123`, confirmPassword: `NewUser@123`, agreeTerms: checked |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open MailHog at `http://localhost:8025` and clear all messages | Inbox is empty. |
| 2 | Complete a successful signup using TC-AUTH-001 steps | Signup succeeds. User redirected to `/profile-setup?verificationPending=true`. |
| 3 | Navigate to MailHog at `http://localhost:8025` | MailHog inbox loads. |
| 4 | Look for an email sent to `newuser@test.com` | An email is present in the inbox addressed to `newuser@test.com`. |
| 5 | Open the email | Email contains a verification link with a `token` query parameter pointing to the `/verify-email` route. |
| 6 | Verify the email subject and body contain appropriate branding and instructions | Subject references email verification or BeeZhub. Body contains a clickable verification link. |

| Field | Value |
|-------|-------|
| **Postconditions** | Verification email received in MailHog with valid token link. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## B. Login (`/login`)

---

### TC-AUTH-013: Successful login with valid student credentials

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-013 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | Student `john.doe@test.com` exists with password `Student@123` and email is verified. |
| **Test Data** | email: `john.doe@test.com`, password: `Student@123` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/login` | Login page loads. Title reads "Welcome back", subtitle reads "Sign in to your account to continue". |
| 2 | Enter `john.doe@test.com` in the Email Address field | Input accepted, no error. |
| 3 | Enter `Student@123` in the Password field | Input accepted, password masked, no error. |
| 4 | Click "Sign In" button | Button shows spinner with "Signing in..." text. Green snackbar appears: **"Login successful!"**. User is redirected to `/dashboard`. |
| 5 | Verify JWT token is stored in localStorage | Open browser DevTools → Application → Local Storage → `http://localhost:4200`. A token/currentUser entry exists. |

| Field | Value |
|-------|-------|
| **Postconditions** | User is logged in and on the student dashboard. JWT token stored in localStorage. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-014: Login with empty email

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-014 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | User is on the login page (`/login`). |
| **Test Data** | email: (empty), password: `Student@123` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/login` | Login page loads. |
| 2 | Leave Email field empty, enter `Student@123` in Password | Password accepted. |
| 3 | Click into Email field and tab out | Validation error appears: **"Email is required"** |
| 4 | Observe the "Sign In" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | User not logged in. Remains on login page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-015: Login with invalid email format

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-015 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | User is on the login page (`/login`). |
| **Test Data** | email: `john.doe.test.com`, password: `Student@123` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/login` | Login page loads. |
| 2 | Enter `john.doe.test.com` in the Email field | Input accepted. |
| 3 | Tab out of the Email field | Validation error appears: **"Please enter a valid email address"** |
| 4 | Observe the "Sign In" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | User not logged in. Remains on login page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-016: Login with empty password

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-016 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | User is on the login page (`/login`). |
| **Test Data** | email: `john.doe@test.com`, password: (empty) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/login` | Login page loads. |
| 2 | Enter `john.doe@test.com` in Email, leave Password empty | Email accepted. |
| 3 | Click into Password field and tab out | Validation error appears: **"Password is required"** |
| 4 | Observe the "Sign In" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | User not logged in. Remains on login page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-017: Login with wrong password

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-017 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | Student `john.doe@test.com` exists with password `Student@123`. |
| **Test Data** | email: `john.doe@test.com`, password: `WrongPass123` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/login` | Login page loads. |
| 2 | Enter `john.doe@test.com` in Email | No error. |
| 3 | Enter `WrongPass123` in Password | No client-side error (meets min length). |
| 4 | Click "Sign In" | Button shows spinner. API returns 401. Red snackbar appears with error message (e.g., "Invalid email or password" or "Login failed. Please try again."). User remains on login page. |

| Field | Value |
|-------|-------|
| **Postconditions** | User not logged in. No token stored. Remains on login page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Record the exact error message from the backend. |

---

### TC-AUTH-018: Login with non-existent email

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-018 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | Email `nonexistent@test.com` is not registered. |
| **Test Data** | email: `nonexistent@test.com`, password: `SomePass123` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/login` | Login page loads. |
| 2 | Enter `nonexistent@test.com` in Email | No client-side error. |
| 3 | Enter `SomePass123` in Password | No client-side error. |
| 4 | Click "Sign In" | API returns error. Red snackbar appears with a generic error message (should not reveal whether the email exists). User remains on login page. |

| Field | Value |
|-------|-------|
| **Postconditions** | User not logged in. Remains on login page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Verify the error message does not explicitly state "email not found" (information leakage concern). |

---

### TC-AUTH-019: Login with unverified email

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-019 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | A user account exists with an unverified email address (e.g., signup completed but verification link not clicked). |
| **Test Data** | email: (unverified account email), password: (correct password for that account) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/login` | Login page loads. |
| 2 | Enter the unverified email and correct password | No client-side errors. |
| 3 | Click "Sign In" | API responds. Either: (a) login succeeds but user is restricted from certain features until verified, or (b) an error/warning message appears indicating email verification is required. Observe and document the actual behavior. |

| Field | Value |
|-------|-------|
| **Postconditions** | Document actual behavior — whether login is allowed or blocked for unverified accounts. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | The system may allow login but restrict access via the `email-verified` guard. Document the exact flow. |

---

### TC-AUTH-020: Login as student redirects to /dashboard

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-020 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | Student `john.doe@test.com` exists with verified email. |
| **Test Data** | email: `john.doe@test.com`, password: `Student@123` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/login` | Login page loads. |
| 2 | Enter `john.doe@test.com` and `Student@123` | No errors. |
| 3 | Click "Sign In" | Login succeeds. Green snackbar: "Login successful!". |
| 4 | Observe the URL in the browser address bar | URL changes to `http://localhost:4200/dashboard`. |
| 5 | Verify student dashboard content loads | Student dashboard page is displayed with student-specific navigation. |

| Field | Value |
|-------|-------|
| **Postconditions** | Student is logged in and viewing `/dashboard`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-021: Login as platform admin redirects to /admin

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-021 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | Platform Admin `platformadmin@test.com` exists with password `Admin@12345`. |
| **Test Data** | email: `platformadmin@test.com`, password: `Admin@12345` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/login` | Login page loads. |
| 2 | Enter `platformadmin@test.com` and `Admin@12345` | No errors. |
| 3 | Click "Sign In" | Login succeeds. Green snackbar: "Login successful!". |
| 4 | Observe the URL in the browser address bar | URL changes to `http://localhost:4200/admin`. |
| 5 | Verify admin dashboard content loads | Admin dashboard/sidebar with admin navigation is displayed. |

| Field | Value |
|-------|-------|
| **Postconditions** | Platform admin is logged in and viewing `/admin`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-022: Login as tenant admin redirects to /admin

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-022 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | Tenant Admin `tenantadmin@test.com` exists with password `Admin@12345`. |
| **Test Data** | email: `tenantadmin@test.com`, password: `Admin@12345` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/login` | Login page loads. |
| 2 | Enter `tenantadmin@test.com` and `Admin@12345` | No errors. |
| 3 | Click "Sign In" | Login succeeds. Green snackbar: "Login successful!". |
| 4 | Observe the URL in the browser address bar | URL changes to `http://localhost:4200/admin`. |
| 5 | Verify admin dashboard loads | Admin dashboard is displayed with tenant admin navigation options. |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant admin is logged in and viewing `/admin`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-023: Login with mustChangePassword=true redirects to forced change password

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-023 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | A user account exists with `mustChangePassword` flag set to `true` in the database. |
| **Test Data** | email: (account with mustChangePassword=true), password: (correct password) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/login` | Login page loads. |
| 2 | Enter the email and password for the account with `mustChangePassword=true` | No client-side errors. |
| 3 | Click "Sign In" | API returns a login response with `mustChangePassword: true`. Yellow/warning snackbar appears: **"You must change your password before continuing."** |
| 4 | Observe the URL in the browser address bar | URL changes to `http://localhost:4200/change-password?forced=true`. |
| 5 | Verify the change password page loads with forced mode | Page title reads **"Password Change Required"** and subtitle reads "For security reasons, you must change your password before continuing." The Cancel button is NOT visible. |

| Field | Value |
|-------|-------|
| **Postconditions** | User is on the forced change password page. Cannot navigate elsewhere until password is changed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Requires backend setup to set mustChangePassword flag. |

---

## C. Email Verification (`/verify-email`)

---

### TC-AUTH-024: Click valid verification link — email verified successfully

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-024 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | A user has signed up and received a verification email. The verification token is valid and not expired. User is logged in (token in localStorage). |
| **Test Data** | A valid verification token extracted from the MailHog email. |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open MailHog at `http://localhost:8025` | MailHog inbox loads. |
| 2 | Open the verification email for the new user | Email body contains a verification link. |
| 3 | Copy the verification link URL | URL is of the form `http://localhost:4200/verify-email?token=<token>`. |
| 4 | Navigate to the verification link in the browser | Verify-email page loads. Left panel shows "Checking your link..." with a spinner. Right panel shows "Verifying your email". |
| 5 | Wait for verification to complete | Left panel updates to show a verified icon and **"You're verified!"** with subtext "Your email address has been confirmed." Right panel shows a green checkmark ring, title **"Email verified"**, and a **"Go to Dashboard"** button. Green snackbar: **"Email verified successfully!"** |
| 6 | Click "Go to Dashboard" | User is redirected to `/dashboard`. |

| Field | Value |
|-------|-------|
| **Postconditions** | User's email is marked as verified in the database. User can access email-verified-only features. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-025: Click expired verification token

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-025 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | A verification token has been generated but has expired (either wait for expiry or manipulate the database to set the token as expired). |
| **Test Data** | An expired verification token. |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/verify-email?token=<expired_token>` | Verify-email page loads. Shows "Checking your link..." spinner initially. |
| 2 | Wait for verification attempt to complete | Left panel shows a link_off icon and **"Link expired or invalid"** with subtext "Verification links are single-use and expire after a short time. Request a new one to continue." Right panel shows a red error ring with X icon, eyebrow text "Verification failed", title **"Couldn't verify your email"**, and an error message from the backend. |
| 3 | Observe the available actions | Two buttons are shown: **"Resend Verification Email"** and **"Back to Login"**. |

| Field | Value |
|-------|-------|
| **Postconditions** | Email remains unverified. User can request a new verification email. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-026: Click already-used verification token

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-026 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | A verification token has already been successfully used (email already verified via TC-AUTH-024). |
| **Test Data** | The same token used in TC-AUTH-024. |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/verify-email?token=<already_used_token>` | Verify-email page loads with spinner. |
| 2 | Wait for verification attempt to complete | Error state is displayed. Left panel: "Link expired or invalid". Right panel: "Couldn't verify your email" with backend error message (e.g., token already used or invalid). |
| 3 | Observe buttons | "Resend Verification Email" and "Back to Login" buttons are visible. |

| Field | Value |
|-------|-------|
| **Postconditions** | No change to account state (email remains verified from first use). |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-027: Click invalid/malformed token

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-027 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | None. |
| **Test Data** | Token: `invalid-garbage-token-12345` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/verify-email?token=invalid-garbage-token-12345` | Verify-email page loads with spinner. |
| 2 | Wait for verification attempt to complete | Error state is displayed: "Couldn't verify your email" with error message from backend (e.g., "Email verification failed. The link may be expired or invalid."). |

| Field | Value |
|-------|-------|
| **Postconditions** | No account affected. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Also test with no token parameter: navigate to `/verify-email` without `?token=`. Expected error message: "No verification token provided. Please check the link in your email." |

---

### TC-AUTH-028: Resend verification email

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-028 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | A user account exists with unverified email. The user is logged in (currentUser in localStorage). MailHog inbox is cleared. |
| **Test Data** | The unverified user's email. |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/verify-email?token=<expired_or_invalid_token>` | Verification fails, error state displayed with "Resend Verification Email" button. |
| 2 | Click "Resend Verification Email" | Green snackbar appears: **"Verification email sent! Please check your inbox."** |
| 3 | Navigate to MailHog at `http://localhost:8025` | MailHog inbox loads. |
| 4 | Check for new verification email | A new verification email is present in the inbox with a new token. |
| 5 | Click the new verification link | Verification succeeds (as in TC-AUTH-024). |

| Field | Value |
|-------|-------|
| **Postconditions** | New verification email received. Email can be verified with the new token. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | If no currentUser in localStorage, the system shows: "Please log in to resend verification email." and redirects to `/login`. |

---

## D. Forgot Password (`/forgot-password`)

---

### TC-AUTH-029: Submit valid email — reset link sent message

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-029 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | User `john.doe@test.com` exists. MailHog inbox is cleared. |
| **Test Data** | email: `john.doe@test.com` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/forgot-password` | Forgot password page loads. Header icon is `lock_reset`, title is "Reset Password", subtitle: "Enter your email address and we'll send you a link to reset your password." |
| 2 | Enter `john.doe@test.com` in the Email Address field | No validation error. |
| 3 | Click "Send Reset Link" | Button shows spinner with "Sending..." text. After success, the form is replaced by a success state: mail icon, message **"We've sent a password reset link to your email address. Please check your inbox and follow the instructions."** with a note about spam folder. "Try Again" and "Back to Login" buttons appear. |

| Field | Value |
|-------|-------|
| **Postconditions** | Success state displayed. Password reset email sent to the user. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-030: Submit empty email on forgot password

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-030 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | User is on the forgot password page (`/forgot-password`). |
| **Test Data** | email: (empty) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/forgot-password` | Forgot password page loads. |
| 2 | Leave the Email field empty | No error yet. |
| 3 | Click into the Email field and tab out | Validation error appears: **"Email is required"** |
| 4 | Observe the "Send Reset Link" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | No reset email sent. User remains on forgot password page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-031: Submit invalid email format on forgot password

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-031 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | User is on the forgot password page (`/forgot-password`). |
| **Test Data** | email: `john.doe.test` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/forgot-password` | Page loads. |
| 2 | Enter `john.doe.test` in the Email field | Input accepted. |
| 3 | Tab out of the Email field | Validation error appears: **"Please enter a valid email address"** |
| 4 | Observe the "Send Reset Link" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | No reset email sent. User remains on forgot password page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-032: Submit non-existent email on forgot password — no information leak

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-032 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | Email `doesnotexist@test.com` is not registered in the system. |
| **Test Data** | email: `doesnotexist@test.com` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/forgot-password` | Page loads. |
| 2 | Enter `doesnotexist@test.com` in the Email field | No error. |
| 3 | Click "Send Reset Link" | The same success state is displayed as for a valid email (TC-AUTH-029): "We've sent a password reset link to your email address." This prevents information leakage about which emails are registered. |
| 4 | Check MailHog at `http://localhost:8025` | No email received for `doesnotexist@test.com` (or verify backend behavior). |

| Field | Value |
|-------|-------|
| **Postconditions** | No reset email actually sent. Success message displayed regardless (security best practice). |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | This is a security requirement — the response must be identical to TC-AUTH-029 to prevent email enumeration. |

---

### TC-AUTH-033: Verify reset email received in MailHog with correct link

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-033 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | TC-AUTH-029 completed successfully. MailHog inbox contains the reset email. |
| **Test Data** | email: `john.doe@test.com` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to MailHog at `http://localhost:8025` | MailHog inbox loads. |
| 2 | Find and open the password reset email for `john.doe@test.com` | Email is present. |
| 3 | Examine the email content | Email contains: a subject line referencing password reset, body with a clickable reset link. |
| 4 | Extract the reset link URL | URL is of the form `http://localhost:4200/reset-password?token=<reset_token>`. |
| 5 | Click or navigate to the reset link | The reset password page loads (see TC-AUTH-034). |

| Field | Value |
|-------|-------|
| **Postconditions** | Reset email verified with valid token link. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## E. Reset Password (`/reset-password`)

---

### TC-AUTH-034: Reset password with valid token and matching passwords

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-034 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | A valid, non-expired password reset token exists (from TC-AUTH-033). |
| **Test Data** | token: (valid token from email), newPassword: `NewStudent@456`, confirmPassword: `NewStudent@456` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/reset-password?token=<valid_token>` | Page loads. Shows "Verifying reset link..." spinner initially. |
| 2 | Wait for token verification | Token is valid. Form appears with title **"Create New Password"**, subtitle "Enter your new password below. Make sure it's at least 8 characters long." Two fields: "New Password" and "Confirm Password". |
| 3 | Enter `NewStudent@456` in the New Password field | Input accepted, no error. |
| 4 | Enter `NewStudent@456` in the Confirm Password field | Input accepted, no error. |
| 5 | Click "Reset Password" | Button shows spinner with "Resetting..." text. Success state appears: green checkmark icon, title **"Password Reset Successfully!"**, message "Your password has been changed. You can now log in with your new password." A **"Go to Login"** button appears. |
| 6 | Click "Go to Login" | User is redirected to `/login`. |
| 7 | Log in with `john.doe@test.com` and `NewStudent@456` | Login succeeds with the new password. |

| Field | Value |
|-------|-------|
| **Postconditions** | Password is changed. Old password no longer works. User can log in with new password. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Remember to reset the password back to `Student@123` for other tests if needed. |

---

### TC-AUTH-035: Reset password with expired token

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-035 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | A password reset token exists but has expired. |
| **Test Data** | token: (expired reset token) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/reset-password?token=<expired_token>` | Page loads. Shows "Verifying reset link..." spinner. |
| 2 | Wait for token verification to complete | Error state displayed: error icon, title **"Invalid or Expired Link"**, message "This password reset link is invalid or has expired. Please request a new password reset link." |
| 3 | Observe available buttons | **"Request New Link"** (links to `/forgot-password`) and **"Back to Login"** (links to `/login`). |

| Field | Value |
|-------|-------|
| **Postconditions** | Password not changed. User directed to request a new reset link. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-036: Reset password with already-used token

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-036 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | The reset token from TC-AUTH-034 has already been used successfully. |
| **Test Data** | token: (already-used reset token from TC-AUTH-034) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/reset-password?token=<used_token>` | Page loads with spinner. |
| 2 | Wait for token verification | Error state displayed: **"Invalid or Expired Link"** with message about the link being invalid or expired. |
| 3 | Observe buttons | "Request New Link" and "Back to Login" buttons available. |

| Field | Value |
|-------|-------|
| **Postconditions** | Password not changed. Token cannot be reused. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-037: Reset password with invalid/malformed token

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-037 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | None. |
| **Test Data** | token: `completely-invalid-token-xyz` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/reset-password?token=completely-invalid-token-xyz` | Page loads with spinner. |
| 2 | Wait for token verification | Error state displayed: **"Invalid or Expired Link"**. |
| 3 | Observe buttons | "Request New Link" and "Back to Login" buttons available. |

| Field | Value |
|-------|-------|
| **Postconditions** | No password changed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-038: Reset password with password less than 8 characters

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-038 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | A valid reset token is active. Reset password form is displayed. |
| **Test Data** | newPassword: `Short1!` (7 chars), confirmPassword: `Short1!` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/reset-password?token=<valid_token>` | Form loads after token verification. |
| 2 | Enter `Short1!` in the New Password field | Input accepted. |
| 3 | Tab out of the New Password field | Validation error appears: **"Password must be at least 8 characters"** |
| 4 | Observe the "Reset Password" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | Password not changed. Form remains with validation error. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-039: Reset password with mismatched passwords

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-039 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | A valid reset token is active. Reset password form is displayed. |
| **Test Data** | newPassword: `NewStudent@456`, confirmPassword: `DifferentPass1` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/reset-password?token=<valid_token>` | Form loads after token verification. |
| 2 | Enter `NewStudent@456` in New Password | No error. |
| 3 | Enter `DifferentPass1` in Confirm Password | Input accepted. |
| 4 | Tab out of Confirm Password field | Validation error appears: **"Passwords do not match"** |
| 5 | Observe the "Reset Password" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | Password not changed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-040: Reset password with empty new password

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-040 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | A valid reset token is active. Reset password form is displayed. |
| **Test Data** | newPassword: (empty), confirmPassword: (empty) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/reset-password?token=<valid_token>` | Form loads after token verification. |
| 2 | Click into the New Password field and tab out without entering anything | Validation error appears: **"Password is required"** |
| 3 | Click into the Confirm Password field and tab out | Validation error appears: **"Please confirm your password"** |
| 4 | Observe the "Reset Password" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | Password not changed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## F. Change Password (`/change-password`)

---

### TC-AUTH-041: Change password with correct current password — success

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-041 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | Student `john.doe@test.com` is logged in with password `Student@123`. |
| **Test Data** | currentPassword: `Student@123`, newPassword: `Student@456`, confirmPassword: `Student@456` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/change-password` | Change password page loads. Title reads "Change Password", subtitle: "Update your password to keep your account secure". Three fields: Current Password, New Password, Confirm New Password. Cancel button is visible (not forced mode). |
| 2 | Enter `Student@123` in Current Password | Input accepted, masked. |
| 3 | Enter `Student@456` in New Password | Input accepted. No validation error. |
| 4 | Enter `Student@456` in Confirm New Password | Input accepted. No error. |
| 5 | Click "Change Password" | Button shows spinner with "Updating..." text. Green snackbar: **"Password changed successfully!"**. User is redirected to `/dashboard` (student role). |
| 6 | Log out and log in with `john.doe@test.com` / `Student@456` | Login succeeds with the new password. |
| 7 | Attempt to log in with old password `Student@123` | Login fails. |

| Field | Value |
|-------|-------|
| **Postconditions** | Password changed. Old password invalid. User redirected based on role. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Reset password back to `Student@123` after test. |

---

### TC-AUTH-042: Change password with wrong current password

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-042 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | Student `john.doe@test.com` is logged in. |
| **Test Data** | currentPassword: `WrongCurrent1`, newPassword: `Student@456`, confirmPassword: `Student@456` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/change-password` | Change password page loads. |
| 2 | Enter `WrongCurrent1` in Current Password | Input accepted. |
| 3 | Enter `Student@456` in New Password and Confirm New Password | No validation errors. |
| 4 | Click "Change Password" | API returns an error. Red snackbar appears with error message (e.g., "Current password is incorrect" or "Failed to change password. Please try again."). User remains on change password page. |

| Field | Value |
|-------|-------|
| **Postconditions** | Password NOT changed. User remains on change password page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Record exact backend error message. |

---

### TC-AUTH-043: Change password with new password less than 8 characters

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-043 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | Student is logged in and on the change password page. |
| **Test Data** | currentPassword: `Student@123`, newPassword: `Short1!` (7 chars), confirmPassword: `Short1!` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/change-password` | Change password page loads. |
| 2 | Enter `Student@123` in Current Password | Accepted. |
| 3 | Enter `Short1!` in New Password | Input accepted. |
| 4 | Tab out of New Password | Validation error appears: **"Password must be at least 8 characters"**. Also a hint reads "Password must be at least 8 characters long". |
| 5 | Observe the "Change Password" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | Password not changed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-044: Change password with mismatched new passwords

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-044 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | Student is logged in and on the change password page. |
| **Test Data** | currentPassword: `Student@123`, newPassword: `Student@456`, confirmPassword: `Student@789` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/change-password` | Change password page loads. |
| 2 | Enter `Student@123` in Current Password | Accepted. |
| 3 | Enter `Student@456` in New Password | No error. |
| 4 | Enter `Student@789` in Confirm New Password | Input accepted. |
| 5 | Tab out of Confirm New Password field | Validation error appears: **"Passwords do not match"** |
| 6 | Observe the "Change Password" button | Button is disabled. |

| Field | Value |
|-------|-------|
| **Postconditions** | Password not changed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-045: Forced change password flow (mustChangePassword=true)

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-045 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | A user account has `mustChangePassword=true`. User has just logged in and been redirected to `/change-password?forced=true` (as per TC-AUTH-023). |
| **Test Data** | currentPassword: (current password), newPassword: `ChangedPass@1`, confirmPassword: `ChangedPass@1` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Observe the change password page at `/change-password?forced=true` | Page title reads **"Password Change Required"**. Subtitle: "For security reasons, you must change your password before continuing." Header icon has `forced` styling. The **Cancel button is NOT visible** (forced mode hides it). |
| 2 | Enter current password in Current Password field | Accepted. |
| 3 | Enter `ChangedPass@1` in New Password | No error. |
| 4 | Enter `ChangedPass@1` in Confirm New Password | No error. |
| 5 | Click "Change Password" | Success. Green snackbar: "Password changed successfully!". User is redirected to the appropriate page based on role (`/admin` for admins, `/dashboard` for students). |
| 6 | Log out and log in again | Login succeeds without forced password change redirect. `mustChangePassword` flag is now `false`. |

| Field | Value |
|-------|-------|
| **Postconditions** | Password changed. `mustChangePassword` flag cleared. User can access the system normally. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## G. Logout & Session

---

### TC-AUTH-046: Logout redirects to /login and clears token

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-046 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | Student `john.doe@test.com` is logged in. |
| **Test Data** | N/A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Verify the user is logged in and on `/dashboard` | Dashboard is displayed. |
| 2 | Click the logout button/option in the navigation | Logout action is triggered (calls `POST /api/auth/logout`). |
| 3 | Observe the browser URL | URL changes to `http://localhost:4200/login`. |
| 4 | Open browser DevTools → Application → Local Storage | JWT token / currentUser entry is removed from localStorage. |
| 5 | Attempt to navigate to `http://localhost:4200/dashboard` directly | User is redirected to `/login` (auth guard prevents access). |

| Field | Value |
|-------|-------|
| **Postconditions** | User is logged out. Token cleared. Protected routes inaccessible. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-AUTH-047: Use blacklisted token after logout returns 401

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-047 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | User has just logged out (TC-AUTH-046). The JWT token from the previous session was saved before logout. |
| **Test Data** | A JWT token captured before logging out. |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Copy the JWT token from localStorage before performing logout | Token string saved. |
| 2 | Perform logout as in TC-AUTH-046 | Logout succeeds. |
| 3 | Using a tool such as Postman or cURL, send a `GET` request to `http://localhost:8080/api/auth/me` with header `Authorization: Bearer <saved_token>` | API returns HTTP `401 Unauthorized`. The token is no longer valid. |

| Field | Value |
|-------|-------|
| **Postconditions** | Token is blacklisted/invalidated server-side. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | This verifies server-side token invalidation, not just client-side cleanup. |

---

### TC-AUTH-048: Token expiry redirects to login with returnUrl

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-048 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | Student is logged in. JWT token is close to expiry or has expired (can be simulated by setting a short token expiry in backend config, or by manually editing the token expiry in localStorage/modifying backend `JWT_EXPIRATION`). |
| **Test Data** | An expired JWT token. |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Wait for the JWT token to expire, or manually set an expired token in localStorage | Token is expired. |
| 2 | Attempt to navigate to or interact with a protected page (e.g., `/dashboard`) | The frontend's token expiry interceptor detects the expired token. |
| 3 | Observe the redirect behavior | User is redirected to `/login`. A returnUrl query parameter may be appended (e.g., `/login?returnUrl=%2Fdashboard`) to allow redirect back after re-login. |
| 4 | Log in again with valid credentials | After successful login, user is redirected to the original page (if returnUrl is supported) or to the default page for their role. |

| Field | Value |
|-------|-------|
| **Postconditions** | Expired token handled gracefully. User re-authenticates and continues. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Document exact behavior of the token expiry interceptor. |

---

## H. Account Management

---

### TC-AUTH-049: Delete own account — confirmation dialog — account deleted

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-049 |
| **Module** | Authentication & Account Management |
| **Priority** | Critical |
| **Preconditions** | Student `jane.smith@test.com` is logged in. Account is expendable for testing purposes. |
| **Test Data** | The logged-in user's account. The `DeleteAccountRequest` payload (may require password confirmation — check implementation). |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to the account/profile settings where account deletion is available | Account deletion option is visible. |
| 2 | Click "Delete Account" or equivalent action | A confirmation dialog appears warning that this action is permanent and irreversible. |
| 3 | Confirm the deletion (provide password if required by the `DeleteAccountRequest` DTO) | API call `POST /api/auth/delete-account` is made. Response: 204 No Content. |
| 4 | Observe the result | User is logged out automatically. Redirected to `/login`. A success message may appear. |
| 5 | Attempt to log in with `jane.smith@test.com` / `Student@123` | Login fails — account no longer exists. |

| Field | Value |
|-------|-------|
| **Postconditions** | Account permanently deleted from the database. User cannot log in. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Re-create the test account after testing if needed for other test cases. |

---

### TC-AUTH-050: GDPR anonymization by platform admin

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-050 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | Platform Admin `platformadmin@test.com` is logged in. A target student account exists (e.g., Student B `jane.smith@test.com` with a known `userId`). |
| **Test Data** | Platform admin credentials. Target userId for anonymization. |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as `platformadmin@test.com` / `Admin@12345` | Admin dashboard loads at `/admin`. |
| 2 | Using Postman or cURL, send `POST http://localhost:8080/api/auth/admin/anonymize/{userId}` with the platform admin's JWT token in the Authorization header | API returns HTTP `204 No Content`. |
| 3 | Verify in the database or via API that the user's PII has been replaced | All personally identifiable information (name, email, etc.) is replaced with placeholder/anonymized values. |
| 4 | Attempt to log in as the anonymized user with original credentials | Login fails — email has been anonymized and password invalidated. |
| 5 | Verify related data integrity | The user record still exists (for referential integrity) but all PII fields are anonymized. Related records (applications, allocations) still reference the user ID. |

| Field | Value |
|-------|-------|
| **Postconditions** | User account anonymized. PII removed. Account cannot be used for login. Related records intact. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | This is an API-only operation (endpoint: `POST /api/auth/admin/anonymize/{userId}`). Requires PLATFORM_ADMIN role. Tenant admins should receive 403 Forbidden. |

---

### TC-AUTH-051: Account lockout after failed login attempts

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-051 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | Student `john.doe@test.com` exists. Account is not currently locked. |
| **Test Data** | email: `john.doe@test.com`, password: `WrongPassword!` (wrong password, used repeatedly) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/login` | Login page loads. |
| 2 | Enter `john.doe@test.com` and `WrongPassword!`, click "Sign In" | Login fails. Error snackbar displayed. |
| 3 | Repeat step 2 multiple times (e.g., 5 times or the configured lockout threshold) | Each attempt fails with an error message. |
| 4 | After exceeding the threshold, attempt to log in again (even with the correct password `Student@123`) | Account is locked. Error message indicates the account is locked (e.g., "Account is locked. Please contact support." or "Too many failed attempts."). |

| Field | Value |
|-------|-------|
| **Postconditions** | Account is locked after exceeding failed login threshold. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | If account lockout is not implemented, document this as "Feature not implemented" and mark N/A. Check backend `AuthService` for lockout logic. |

---

### TC-AUTH-052: Unlock locked account (platform admin)

| Field | Value |
|-------|-------|
| **ID** | TC-AUTH-052 |
| **Module** | Authentication & Account Management |
| **Priority** | High |
| **Preconditions** | Student `john.doe@test.com` account is locked (from TC-AUTH-051). Platform admin is available. |
| **Test Data** | Platform admin: `platformadmin@test.com` / `Admin@12345`. Locked user: `john.doe@test.com`. |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as platform admin `platformadmin@test.com` / `Admin@12345` | Admin dashboard loads. |
| 2 | Navigate to user management section in the admin panel | User list is displayed. |
| 3 | Find `john.doe@test.com` in the user list | User is shown with a locked/disabled status indicator. |
| 4 | Click "Unlock" or equivalent action for the locked user | Account is unlocked. Status indicator changes to active/unlocked. |
| 5 | Log out as admin | Admin logged out. |
| 6 | Log in as `john.doe@test.com` / `Student@123` | Login succeeds. Account is no longer locked. |

| Field | Value |
|-------|-------|
| **Postconditions** | Account unlocked. Student can log in normally. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | If account lockout/unlock is not implemented, document as "Feature not implemented" and mark N/A. The exact admin UI path depends on the user management feature implementation. |

---

## Summary

| Section | Test Cases | IDs |
|---------|-----------|-----|
| A. Signup | 12 | TC-AUTH-001 to TC-AUTH-012 |
| B. Login | 11 | TC-AUTH-013 to TC-AUTH-023 |
| C. Email Verification | 5 | TC-AUTH-024 to TC-AUTH-028 |
| D. Forgot Password | 5 | TC-AUTH-029 to TC-AUTH-033 |
| E. Reset Password | 7 | TC-AUTH-034 to TC-AUTH-040 |
| F. Change Password | 5 | TC-AUTH-041 to TC-AUTH-045 |
| G. Logout & Session | 3 | TC-AUTH-046 to TC-AUTH-048 |
| H. Account Management | 4 | TC-AUTH-049 to TC-AUTH-052 |
| **Total** | **52** | |

---

**Document Version:** 1.0
**Last Updated:** 2026-05-17
**Author:** QA Team
