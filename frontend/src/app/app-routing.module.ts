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
  { path: '**', redirectTo: '/dashboard' }
];