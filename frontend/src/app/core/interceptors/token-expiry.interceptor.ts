import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const tokenExpiryInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (authService.isTokenExpired()) {
        authService.logout();
        router.navigate(['/login']);
        return throwError(() => new Error('Token expired'));
    }
    return next(req);
};
