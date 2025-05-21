import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'signup',
    loadComponent: () => import('./features/auth/signup/signup.component').then(m => m.SignupComponent)
  },
  {
    path: 'profile-setup',
    loadComponent: () => import('./features/auth/profile-setup/profile-setup.component').then(m => m.ProfileSetupComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'document-upload',
    loadComponent: () => import('./features/auth/document-upload/document-upload.component').then(m => m.DocumentUploadComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'application-review',
    loadComponent: () => import('./features/auth/application-review/application-review.component').then(m => m.ApplicationReviewComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'application-confirmation',
    loadComponent: () => import('./features/auth/application-confirmation/application-confirmation.component').then(m => m.ApplicationConfirmationComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'contact-support',
    loadComponent: () => import('./features/dashboard/contact-support/contact-support.component').then(m => m.ContactSupportComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'applications',
    loadComponent: () => import('./features/dashboard/applications/applications.component').then(m => m.ApplicationsComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'applications/new',
    loadComponent: () => import('./features/dashboard/applications/new-application/new-application.component').then(m => m.NewApplicationComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'applications/:id',
    loadComponent: () => import('./features/dashboard/applications/application-details/application-details.component').then(m => m.ApplicationDetailsComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'documents',
    loadComponent: () => import('./features/dashboard/documents/documents.component').then(m => m.DocumentsComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'profile',
    loadComponent: () => import('./features/dashboard/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [AuthGuard]
  },
  { path: '**', redirectTo: '/dashboard' }
];