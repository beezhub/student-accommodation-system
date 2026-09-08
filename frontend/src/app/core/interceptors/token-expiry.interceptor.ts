import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { TokenStorageService } from '../services/token-storage.service';
import { isOurApiRequest } from './api-request';

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
    '/api/public/owner/',
  ];

  // A third-party URL is in none of the lists above, so without this an expired token turned
  // typing in the address field into a forced sign-out.
  if (!isOurApiRequest(req.url)) {
    return next(req);
  }

  const isPublicEndpoint = publicEndpoints.some((endpoint) => req.url.includes(endpoint));

  const token = tokenStorage.getToken();
  if (!isPublicEndpoint && token && !tokenStorage.isTokenValid()) {
    // Read the URL first: logout() navigates to /home, and only deferred navigation timing
    // makes a read after it happen to still return the old value (GH-150).
    const returnUrl = router.url;
    authService.logout();
    router.navigate(['/login'], { queryParams: { returnUrl } });
    return throwError(() => new Error('Token expired'));
  }
  return next(req);
};
