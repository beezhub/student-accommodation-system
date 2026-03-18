import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { TokenStorageService } from '../services/token-storage.service';

export const tokenExpiryInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const tokenStorage = inject(TokenStorageService);

  // Skip token expiry check for public auth endpoints
  const publicEndpoints = [
    '/api/auth/login',
    '/api/auth/signup',
    '/api/auth/forgot-password',
    '/api/auth/verify-reset-token',
    '/api/auth/reset-password',
    '/api/auth/verify-email',
    '/api/auth/resend-verification',
  ];

  const isPublicEndpoint = publicEndpoints.some((endpoint) => req.url.includes(endpoint));

  const token = tokenStorage.getToken();
  if (!isPublicEndpoint && token && !tokenStorage.isTokenValid()) {
    authService.logout();
    router.navigate(['/login']);
    return throwError(() => new Error('Token expired'));
  }
  return next(req);
};
