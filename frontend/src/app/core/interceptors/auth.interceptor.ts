import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenStorageService } from '../services/token-storage.service';
import { isOurApiRequest } from './api-request';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStorage = inject(TokenStorageService);

  const publicEndpoints = [
    '/api/auth/login',
    '/api/auth/signup',
    '/api/auth/forgot-password',
    '/api/auth/verify-reset-token',
    '/api/auth/reset-password',
    '/api/auth/verify-email',
    '/api/auth/resend-verification',
  ];

  // Allow-list, not deny-list. The address autocomplete used to call a third-party host through
  // this same HttpClient, and treating everything unrecognised as ours sent an administrator
  // bearer token to nominatim.openstreetmap.org. That call moved behind our own API in GH-152;
  // the allow-list stays, so the next third-party call is safe by default rather than by
  // someone remembering to exclude it.
  if (!isOurApiRequest(req.url)) {
    return next(req);
  }

  const isPublicEndpoint = publicEndpoints.some((endpoint) => req.url.includes(endpoint));

  if (isPublicEndpoint) {
    return next(req);
  }

  const token = tokenStorage.getToken();

  if (token) {
    const cloned = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`),
    });
    return next(cloned);
  }

  return next(req);
};
