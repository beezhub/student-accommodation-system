import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const tokenExpiryInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    // Skip token expiry check for public auth endpoints
    const publicEndpoints = [
        '/api/auth/login',
        '/api/auth/signup',
        '/api/auth/forgot-password',
        '/api/auth/verify-reset-token',
        '/api/auth/reset-password',
        '/api/auth/verify-email',
        '/api/auth/resend-verification'
    ];

    const isPublicEndpoint = publicEndpoints.some(endpoint => req.url.includes(endpoint));

    if (!isPublicEndpoint && authService.isTokenExpired()) {
        authService.logout();
        router.navigate(['/login']);
        return throwError(() => new Error('Token expired'));
    }
    return next(req);
};
