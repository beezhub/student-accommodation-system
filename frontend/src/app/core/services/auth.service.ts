import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';
import { SignupData } from '../models/signup-data.model';
import { InstitutionService } from './institution.service';
import { YearOfStudyService } from './year-of-study.service';
import { Router } from '@angular/router';
import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  isLoggedIn(): boolean {
    const token = localStorage.getItem('token');
    return token !== null && !this.isTokenExpired();
  }
  private apiUrl = `${environment.apiUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private institutionService: InstitutionService,
    private yearOfStudyService: YearOfStudyService,
    private router: Router,
    private notificationService: NotificationService
  ) {
    window.addEventListener('storage', (event) => {
      if (event.key === 'currentUser') {
        const updatedUser = event.newValue ? JSON.parse(event.newValue) : null;
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
        localStorage.setItem('token', response.token);
        localStorage.setItem('currentUser', JSON.stringify(response.user));
        this.currentUserSubject.next(response.user);

        // Start notification polling after successful signup
        this.notificationService.startPolling();

        return response.user;
      })
    );
  }

  uploadDocuments(files: FormData): Observable<any> {
    return this.http.post(`${environment.apiUrl}/documents/upload-multiple`, files);
  }

  submitApplication(applicationData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/submit-application`, applicationData);
  }

  logout(): void {
    // Stop notification polling before logout
    this.notificationService.stopPolling();

    localStorage.removeItem('currentUser');
    localStorage.removeItem('token');
    this.currentUserSubject.next(null);
    this.institutionService.clearCache();
    this.yearOfStudyService.clearCache();
    this.router.navigate(['/home']);
  }
  // Login a user
  login(data: any): Observable<User> {
    return this.http.post<{ token: string; user: User }>(`${this.apiUrl}/login`, data).pipe(
      map((response) => {
        localStorage.setItem('token', response.token);
        localStorage.setItem('currentUser', JSON.stringify(response.user));
        this.currentUserSubject.next(response.user);

        // Start notification polling after successful login
        this.notificationService.startPolling();

        return response.user;
      })
    );
  }

  isTokenExpired(): boolean {
    const token = localStorage.getItem('token');
    if (!token) return true;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expiry = payload.exp;
      return Math.floor(Date.now() / 1000) > expiry;
    } catch (e) {
      return true;
    }
  }

  changePassword(currentPassword: string, newPassword: string): Observable<any> {
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
            currentUser.mustChangePassword = false;
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
            this.currentUserSubject.next(currentUser);
          }
          return response;
        })
      );
  }

  deleteAccount(password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/delete-account`, { password });
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
      currentUser.emailVerified = verified;
      localStorage.setItem('currentUser', JSON.stringify(currentUser));
      this.currentUserSubject.next(currentUser);
    }
  }
}
