import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { AdminGuard } from './core/guards/admin.guard';
import { StudentGuard } from './core/guards/student.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  {
    path: 'home',
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'signup',
    loadComponent: () => import('./features/auth/signup/signup.component').then(m => m.SignupComponent)
  },
  {
    path: 'change-password',
    loadComponent: () => import('./features/auth/change-password/change-password.component').then(m => m.ChangePasswordComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'profile-setup',
    loadComponent: () => import('./features/auth/profile-setup/profile-setup.component').then(m => m.ProfileSetupComponent),
    canActivate: [AuthGuard, StudentGuard]
  },
  {
    path: 'document-upload',
    loadComponent: () => import('./features/auth/document-upload/document-upload.component').then(m => m.DocumentUploadComponent),
    canActivate: [AuthGuard, StudentGuard]
  },
  {
    path: 'application-review',
    loadComponent: () => import('./features/auth/application-review/application-review.component').then(m => m.ApplicationReviewComponent),
    canActivate: [AuthGuard, StudentGuard]
  },
  {
    path: 'application-confirmation/:applicationId',
    loadComponent: () => import('./features/auth/application-confirmation/application-confirmation.component').then(m => m.ApplicationConfirmationComponent),
    canActivate: [AuthGuard, StudentGuard]
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [AuthGuard, StudentGuard]
  },
  {
    path: 'contact-support',
    loadComponent: () => import('./features/dashboard/contact-support/contact-support.component').then(m => m.ContactSupportComponent),
    canActivate: [AuthGuard, StudentGuard]
  },
  {
    path: 'applications',
    loadComponent: () => import('./features/dashboard/applications/applications.component').then(m => m.ApplicationsComponent),
    canActivate: [AuthGuard, StudentGuard]
  },
  {
    path: 'applications/new',
    loadComponent: () => import('./features/dashboard/applications/new-application/new-application.component').then(m => m.NewApplicationComponent),
    canActivate: [AuthGuard, StudentGuard]
  },
  {
    path: 'applications/:id',
    loadComponent: () => import('./features/dashboard/applications/application-details/application-details.component').then(m => m.ApplicationDetailsComponent),
    canActivate: [AuthGuard, StudentGuard]
  },
  {
    path: 'documents',
    loadComponent: () => import('./features/dashboard/documents/documents.component').then(m => m.DocumentsComponent),
    canActivate: [AuthGuard, StudentGuard]
  },
  {
    path: 'profile',
    loadComponent: () => import('./features/dashboard/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [AuthGuard, StudentGuard]
  },
  {
    path: 'rooms',
    loadComponent: () => import('./features/student/room-browser/room-browser.component').then(m => m.RoomBrowserComponent),
    canActivate: [AuthGuard, StudentGuard]
  },
  {
    path: 'admin',
    canActivate: [AuthGuard, AdminGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/admin/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent)
      },
      {
        path: 'applications',
        loadComponent: () => import('./features/admin/application-review/application-review.component').then(m => m.ApplicationReviewComponent)
      },
      {
        path: 'documents',
        loadComponent: () => import('./features/admin/document-verification/document-verification.component').then(m => m.DocumentVerificationComponent)
      },
      {
        path: 'users',
        loadComponent: () => import('./features/admin/user-management/user-management.component').then(m => m.UserManagementComponent)
      },
      {
        path: 'buildings',
        loadComponent: () => import('./features/admin/building-management/building-management.component').then(m => m.BuildingManagementComponent)
      },
      {
        path: 'rooms',
        loadComponent: () => import('./features/admin/room-management/room-management.component').then(m => m.RoomManagementComponent)
      },
      {
        path: 'room-allocations',
        loadComponent: () => import('./features/admin/room-allocation/room-allocation.component').then(m => m.RoomAllocationComponent)
      },
      {
        path: 'activity-logs',
        loadComponent: () => import('./features/admin/activity-logs/activity-logs.component').then(m => m.ActivityLogsComponent)
      }
    ]
  },
  { path: '**', redirectTo: '/home' }
];