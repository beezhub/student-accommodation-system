import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { AdminGuard } from './core/guards/admin.guard';
import { StudentGuard } from './core/guards/student.guard';
import { EmailVerifiedGuard } from './core/guards/email-verified.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  {
    path: 'home',
    loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent),
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'signup',
    loadComponent: () =>
      import('./features/auth/signup/signup.component').then((m) => m.SignupComponent),
  },
  {
    path: 'verify-email',
    loadComponent: () =>
      import('./features/auth/verify-email/verify-email.component').then(
        (m) => m.VerifyEmailComponent
      ),
  },
  {
    path: 'email-verification-required',
    loadComponent: () =>
      import(
        './features/auth/email-verification-required/email-verification-required.component'
      ).then((m) => m.EmailVerificationRequiredComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'change-password',
    loadComponent: () =>
      import('./features/auth/change-password/change-password.component').then(
        (m) => m.ChangePasswordComponent
      ),
    canActivate: [AuthGuard],
  },
  {
    path: 'profile-setup',
    loadComponent: () =>
      import('./features/auth/profile-setup/profile-setup.component').then(
        (m) => m.ProfileSetupComponent
      ),
    canActivate: [AuthGuard, StudentGuard],
  },
  {
    path: 'application-submission',
    loadComponent: () =>
      import('./features/auth/application-submission/application-submission.component').then(
        (m) => m.ApplicationSubmissionComponent
      ),
    canActivate: [AuthGuard, StudentGuard],
  },
  {
    path: 'document-upload',
    loadComponent: () =>
      import('./features/auth/document-upload/document-upload.component').then(
        (m) => m.DocumentUploadComponent
      ),
    canActivate: [AuthGuard, StudentGuard],
  },
  {
    path: 'application-review',
    loadComponent: () =>
      import('./features/auth/application-review/application-review.component').then(
        (m) => m.ApplicationReviewComponent
      ),
    canActivate: [AuthGuard, StudentGuard],
  },
  {
    path: 'application-confirmation/:applicationId',
    loadComponent: () =>
      import('./features/auth/application-confirmation/application-confirmation.component').then(
        (m) => m.ApplicationConfirmationComponent
      ),
    canActivate: [AuthGuard, StudentGuard],
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
    canActivate: [AuthGuard, StudentGuard, EmailVerifiedGuard],
  },
  {
    path: 'contact-support',
    loadComponent: () =>
      import('./features/dashboard/contact-support/contact-support.component').then(
        (m) => m.ContactSupportComponent
      ),
    canActivate: [AuthGuard, StudentGuard, EmailVerifiedGuard],
  },
  {
    path: 'applications',
    loadComponent: () =>
      import('./features/dashboard/applications/applications.component').then(
        (m) => m.ApplicationsComponent
      ),
    canActivate: [AuthGuard, StudentGuard, EmailVerifiedGuard],
  },
  {
    path: 'applications/new',
    loadComponent: () =>
      import('./features/dashboard/applications/new-application/new-application.component').then(
        (m) => m.NewApplicationComponent
      ),
    canActivate: [AuthGuard, StudentGuard, EmailVerifiedGuard],
  },
  {
    path: 'applications/:id',
    loadComponent: () =>
      import(
        './features/dashboard/applications/application-details/application-details.component'
      ).then((m) => m.ApplicationDetailsComponent),
    canActivate: [AuthGuard, StudentGuard, EmailVerifiedGuard],
  },
  {
    path: 'documents',
    loadComponent: () =>
      import('./features/dashboard/documents/documents.component').then(
        (m) => m.DocumentsComponent
      ),
    canActivate: [AuthGuard, StudentGuard, EmailVerifiedGuard],
  },
  {
    path: 'profile',
    loadComponent: () =>
      import('./features/dashboard/profile/profile.component').then((m) => m.ProfileComponent),
    canActivate: [AuthGuard, StudentGuard, EmailVerifiedGuard],
  },
  // Room browser route removed from MVP - feature deferred
  // Component preserved at features/student/room-browser/ for future use
  {
    path: 'admin',
    canActivate: [AuthGuard, AdminGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/admin/admin-dashboard/admin-dashboard.component').then(
            (m) => m.AdminDashboardComponent
          ),
      },
      {
        path: 'applications',
        loadComponent: () =>
          import('./features/admin/application-review/application-review.component').then(
            (m) => m.ApplicationReviewComponent
          ),
      },
      {
        path: 'documents',
        loadComponent: () =>
          import('./features/admin/document-verification/document-verification.component').then(
            (m) => m.DocumentVerificationComponent
          ),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./features/admin/user-management/user-management.component').then(
            (m) => m.UserManagementComponent
          ),
      },
      {
        path: 'buildings',
        loadComponent: () =>
          import('./features/admin/building-management/building-management.component').then(
            (m) => m.BuildingManagementComponent
          ),
      },
      {
        path: 'rooms',
        loadComponent: () =>
          import('./features/admin/room-management/room-management.component').then(
            (m) => m.RoomManagementComponent
          ),
      },
      {
        path: 'room-allocations',
        loadComponent: () =>
          import('./features/admin/room-allocation/room-allocation.component').then(
            (m) => m.RoomAllocationComponent
          ),
      },
      {
        path: 'activity-logs',
        loadComponent: () =>
          import('./features/admin/activity-logs/activity-logs.component').then(
            (m) => m.ActivityLogsComponent
          ),
      },
      {
        path: 'institutions',
        loadComponent: () =>
          import('./features/admin/institution-management/institution-management.component').then(
            (m) => m.InstitutionManagementComponent
          ),
      },
      {
        path: 'document-types',
        loadComponent: () =>
          import(
            './features/admin/document-type-management/document-type-management.component'
          ).then((m) => m.DocumentTypeManagementComponent),
      },
      {
        path: 'year-of-study',
        loadComponent: () =>
          import(
            './features/admin/year-of-study-management/year-of-study-management.component'
          ).then((m) => m.YearOfStudyManagementComponent),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/admin/system-settings/system-settings.component').then(
            (m) => m.SystemSettingsComponent
          ),
      },
    ],
  },
  { path: '**', redirectTo: '/home' },
];
