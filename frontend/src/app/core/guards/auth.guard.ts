import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard {
  private router = inject(Router);
  private authService = inject(AuthService);

  canActivate(): boolean {
    if (!this.authService.isTokenExpired()) {
      return true;
    } else {
      this.router.navigate(['/login']);
      return false;
    }
  }
}
