import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
    providedIn: 'root'
})
export class AuthGuard {
    constructor(
        private router: Router,
        private authService: AuthService
    ) {}

    canActivate(): boolean {
        const currentUser = this.authService.currentUserValue;
        if (currentUser) {
            return true;
        }

        this.router.navigate(['/login']);
        return false;
    }
}