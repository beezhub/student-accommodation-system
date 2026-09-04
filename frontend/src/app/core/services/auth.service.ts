import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';
import { SignupData } from '../models/signup-data.model';
import { InstitutionService } from './institution.service';
import { YearOfStudyService } from './year-of-study.service';
import { Router } from '@angular/router';
import { NotificationService } from './notification.service';
import { TokenStorageService } from './token-storage.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private institutionService = inject(InstitutionService);
  private yearOfStudyService = inject(YearOfStudyService);
  private router = inject(Router);
  private notificationService = inject(NotificationService);

  private tokenStorage = inject(TokenStorageService);

  isLoggedIn(): boolean {
    return this.tokenStorage.isTokenValid();
  }
  private apiUrl = `${environment.apiUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser = this.currentUserSubject.asObservable();

  constructor() {
    // Initialize from localStorage on app start
    const storedUser = localStorage.getItem('currentUser');
    if (storedUser) {
      try {
        this.currentUserSubject.next(JSON.parse(storedUser));
        // Re-initialize notification stream on page refresh if user is already logged in
        if (this.isLoggedIn()) {
          this.notificationService.initializeNotifications();
        }
      } catch {
        localStorage.removeItem('currentUser');
      }
    }

    window.addEventListener('storage', (event) => {
      if (event.key === 'currentUser') {
        let updatedUser = null;
        try {
          updatedUser = event.newValue ? JSON.parse(event.newValue) : null;
        } catch {
          // Ignore malformed JSON from other tabs
        }
        this.currentUserSubject.next(updatedUser);
      }
    });
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  signup(data: SignupData): Observable<User> {
    return this.http.post<{ token: string; user: User }>(`${this.apiUrl}/signup`, data).pipe(
      map((response) => {
        const tenantId = this.extractTenantIdFromToken(response.token);
        if (tenantId !== undefined) {
          response.user.tenantId = tenantId;
        }
        this.tokenStorage.setToken(response.token);
        localStorage.setItem('currentUser', JSON.stringify(response.user));
        this.currentUserSubject.next(response.user);

        // Start notification stream after successful signup
        this.notificationService.initializeNotifications();

        return response.user;
      })
    );
  }

  uploadDocuments(files: FormData): Observable<any> {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return this.http.post(`${environment.apiUrl}/documents/upload-multiple`, files);
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  submitApplication(applicationData: any): Observable<any> {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return this.http.post(`${this.apiUrl}/submit-application`, applicationData);
  }

  logout(): void {
    // Stop notifications before logout
    this.notificationService.stopNotifications();

    localStorage.removeItem('currentUser');
    this.tokenStorage.removeToken();
    this.currentUserSubject.next(null);
    this.institutionService.clearCache();
    this.yearOfStudyService.clearCache();
    this.router.navigate(['/home']);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  }
  // Login a user
  login(data: any): Observable<User> {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return this.http.post<{ token: string; user: User }>(`${this.apiUrl}/login`, data).pipe(
      map((response) => {
        const tenantId = this.extractTenantIdFromToken(response.token);
        if (tenantId !== undefined) {
          response.user.tenantId = tenantId;
        }
        this.tokenStorage.setToken(response.token);
        localStorage.setItem('currentUser', JSON.stringify(response.user));
        this.currentUserSubject.next(response.user);

        // Start notification stream after successful login
        this.notificationService.initializeNotifications();

        return response.user;
      })
    );
  }

  private extractTenantIdFromToken(token: string): number | undefined {
    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload));
      return decoded.tenantId;
    } catch {
      return undefined;
    }
  }

  isTokenExpired(): boolean {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return !this.tokenStorage.isTokenValid();
  }

  changePassword(currentPassword: string, newPassword: string): Observable<any> {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return this.http
      .post(`${this.apiUrl}/change-password`, {
        currentPassword,
        newPassword,
      })
      .pipe(
        map((response) => {
          // Update the user in localStorage to clear mustChangePassword flag
          const currentUser = this.currentUserValue;
          if (currentUser) {
            const updated = { ...currentUser, mustChangePassword: false };
            localStorage.setItem('currentUser', JSON.stringify(updated));
            this.currentUserSubject.next(updated);
          }
          return response;
        })
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      );
  }

  deleteAccount(password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/delete-account`, { password });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  }

  resendVerificationEmail(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/resend-verification`, { email });
  }

  verifyEmail(token: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/verify-email`, { token });
  }

  isEmailVerified(): boolean {
    const currentUser = this.currentUserValue;
    return currentUser?.emailVerified === true;
  }

  updateEmailVerificationStatus(verified: boolean): void {
    const currentUser = this.currentUserValue;
    if (currentUser) {
      const updated = { ...currentUser, emailVerified: verified };
      localStorage.setItem('currentUser', JSON.stringify(updated));
      this.currentUserSubject.next(updated);
    }
  }

  // Password Reset Methods
  forgotPassword(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/forgot-password`, { email });
  }

  verifyResetToken(token: string): Observable<{ valid: boolean; message: string }> {
    return this.http.post<{ valid: boolean; message: string }>(
      `${this.apiUrl}/verify-reset-token`,
      { token }
    );
  }

  resetPassword(token: string, newPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/reset-password`, {
      token,
      newPassword,
    });
  }
}
