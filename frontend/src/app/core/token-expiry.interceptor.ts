import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from './services/auth.service';

@Injectable()
export class TokenExpiryInterceptor implements HttpInterceptor {
    constructor(private authService: AuthService, private router: Router) {}

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        if (this.authService.isTokenExpired()) {
            this.authService.logout();
            this.router.navigate(['/login']);
            return throwError(() => new Error('Token expired'));
        }
        return next.handle(req);
    }
}
