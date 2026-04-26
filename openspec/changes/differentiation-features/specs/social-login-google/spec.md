## ADDED Requirements

### Requirement: Google Sign-In button on login page
The login page SHALL display a "Sign in with Google" button below the email/password form, visually separated by an "or" divider. The button MUST follow Google's brand guidelines (Google logo, white background, "Sign in with Google" text).

#### Scenario: User clicks Google Sign-In
- **WHEN** a user clicks the "Sign in with Google" button on the login page
- **THEN** the browser redirects to Google's OAuth2 authorization endpoint

#### Scenario: Google Sign-In button hidden when feature disabled
- **WHEN** the system setting `SOCIAL_LOGIN_ENABLED` is `false`
- **THEN** the Google Sign-In button and "or" divider SHALL NOT be rendered on the login page

### Requirement: Google Sign-In button on signup page
The signup page SHALL display a "Sign up with Google" button with the same visual treatment as the login page.

#### Scenario: User clicks Google Sign-Up
- **WHEN** a user clicks the "Sign up with Google" button on the signup page
- **THEN** the browser redirects to Google's OAuth2 authorization endpoint

### Requirement: OAuth2 callback creates or links account
The backend SHALL handle the Google OAuth2 callback at `/oauth2/callback/google`. If no `app_user` exists with the Google-provided email, the system MUST create a new `AppUser` with role `STUDENT`, the Google subject ID stored in a new `google_subject_id` column, and the user's name from the Google profile. If an `app_user` already exists with that email, the system MUST link the Google subject ID to the existing account.

#### Scenario: New user authenticates via Google
- **WHEN** a user completes Google OAuth2 and no `app_user` exists with their Google email
- **THEN** the system creates a new `AppUser` with role `STUDENT`, stores the Google subject ID, and issues a JWT token

#### Scenario: Existing user authenticates via Google for the first time
- **WHEN** a user completes Google OAuth2 and an `app_user` with their email already exists but has no `google_subject_id`
- **THEN** the system links the Google subject ID to the existing account and issues a JWT token

#### Scenario: Returning Google user authenticates
- **WHEN** a user completes Google OAuth2 and their `google_subject_id` is already stored
- **THEN** the system issues a JWT token for the linked account

### Requirement: JWT issued after OAuth2 callback
After successful Google OAuth2 authentication, the system SHALL issue a JWT token using the same token generation logic as email/password login. The callback endpoint MUST redirect to the frontend with the JWT token as a URL fragment parameter (`/auth/callback#token=...`).

#### Scenario: Frontend receives JWT after Google login
- **WHEN** the OAuth2 callback completes successfully
- **THEN** the browser is redirected to `/auth/callback#token={jwt}` and the frontend extracts and stores the token

### Requirement: Google profile photo imported
When a new account is created via Google OAuth2, the system SHALL store the Google profile photo URL in the user's profile. This URL is used as the avatar in the frontend.

#### Scenario: Profile photo stored on new Google account
- **WHEN** a new account is created via Google OAuth2 and Google provides a profile photo URL
- **THEN** the `profile_photo_url` field on the user is set to the Google-provided URL

#### Scenario: No profile photo from Google
- **WHEN** a new account is created via Google OAuth2 and no profile photo is provided
- **THEN** the `profile_photo_url` field remains null and the frontend displays the default avatar

### Requirement: Social login feature flag
The system SHALL respect a `SOCIAL_LOGIN_ENABLED` system setting (default `false`). When disabled, the OAuth2 login endpoints MUST return `404` and the frontend MUST hide all Google Sign-In UI elements.

#### Scenario: Feature flag disabled
- **WHEN** `SOCIAL_LOGIN_ENABLED` is `false` and a request is made to `/oauth2/authorization/google`
- **THEN** the system returns `404 Not Found`

#### Scenario: Feature flag enabled
- **WHEN** `SOCIAL_LOGIN_ENABLED` is `true` and a request is made to `/oauth2/authorization/google`
- **THEN** the system redirects to Google's OAuth2 authorization endpoint
