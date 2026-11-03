import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';
import { SignupData } from '../models/signup-data.model';
import { InstitutionService } from './institution.service';
import { YearOfStudyService } from './year-of-study.service';
import { Router } from '@angular/router';
import { NotificationService } from './notification.service';
import { TokenStorageService } from './token-storage.service';

/** What the login form sends. Was `any`, so a typo in a field name compiled (GH-158). */
export interface LoginData {
  email: string;
  password: string;
}

/** The `{ message }` body the backend returns from commands that have nothing else to say. */
export interface MessageResponse {
  message: string;
}

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
      if (!this.tokenStorage.isTokenValid()) {
        // The stored user used to be emitted whatever the token said, so an expired session
        // still rendered the signed-in toolbar until the guard redirect resolved, and the name,
        // email and role stayed in localStorage indefinitely (GH-151).
        this.clearLocalSession();
      } else {
        try {
          this.currentUserSubject.next(JSON.parse(storedUser));
          // Re-initialize notification stream on page refresh if user is already logged in
          this.notificationService.initializeNotifications();
        } catch {
          this.clearLocalSession();
        }
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
    return this.http
      .post<{ token: string; user: User }>(`${this.apiUrl}/signup`, data, {
        headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
      })
      .pipe(
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

  uploadDocuments(files: FormData): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(
      `${environment.apiUrl}/documents/upload-multiple`,
      files
    );
  }
  submitApplication(applicationData: unknown): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.apiUrl}/submit-application`, applicationData);
  }

  logout(): void {
    // Stop notifications before logout
    this.notificationService.stopNotifications();

    // Tell the backend to blacklist this token, otherwise a stolen copy stays valid for the
    // rest of its lifetime. Local state is cleared either way, so a failed call cannot strand
    // the user in a half-logged-out state.
    const hadToken = !!this.tokenStorage.getToken();
    if (hadToken) {
      this.http
        .post(`${this.apiUrl}/logout`, {}, { headers: { 'X-Silent-Error': 'true' } })
        .subscribe({
          error: (err) =>
            console.error('Logout request failed; clearing local session anyway', err),
        });
    }

    this.clearLocalSession();
    this.router.navigate(['/home']);
  }

  /** Drops every trace of the session held in the browser. */
  private clearLocalSession(): void {
    localStorage.removeItem('currentUser');
    this.tokenStorage.removeToken();
    this.currentUserSubject.next(null);
    this.institutionService.clearCache();
    this.yearOfStudyService.clearCache();
  }

  /**
   * Ends the local session right after a successful `changePassword` call.
   *
   * `changePassword` blacklists the token that was presented to authenticate it, so the copy
   * still sitting in the browser is already dead. Calling `logout()` here would ask the backend
   * to blacklist that same dead token a second time, which answers 401 and drags the caller
   * through the same "session expired" redirect this method exists to avoid (GH-291). There is
   * nothing left to tell the backend, so this only clears the local copy.
   */
  endSessionAfterPasswordChange(): void {
    this.notificationService.stopNotifications();
    this.clearLocalSession();
  }

  // Login a user
  login(data: LoginData): Observable<User> {
    return this.http
      .post<{ token: string; user: User }>(`${this.apiUrl}/login`, data, {
        headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
      })
      .pipe(
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
    return !this.tokenStorage.isTokenValid();
  }

  changePassword(currentPassword: string, newPassword: string): Observable<MessageResponse> {
    return this.http
      .post<MessageResponse>(
        `${this.apiUrl}/change-password`,
        {
          currentPassword,
          newPassword,
        },
        { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
      )
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
      );
  }

  deleteAccount(password: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.apiUrl}/delete-account`, { password });
  }

  resendVerificationEmail(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${this.apiUrl}/resend-verification`,
      { email },
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  verifyEmail(token: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${this.apiUrl}/verify-email`,
      { token },
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
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
    return this.http.post<{ message: string }>(
      `${this.apiUrl}/forgot-password`,
      { email },
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  verifyResetToken(token: string): Observable<{ valid: boolean; message: string }> {
    return this.http.post<{ valid: boolean; message: string }>(
      `${this.apiUrl}/verify-reset-token`,
      { token },
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  resetPassword(token: string, newPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${this.apiUrl}/reset-password`,
      {
        token,
        newPassword,
      },
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }
}
