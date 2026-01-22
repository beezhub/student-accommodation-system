import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Application } from '../models/application.model';

@Injectable({
  providedIn: 'root',
})
export class ApplicationService {
  private apiUrl = `${environment.apiUrl}/applications`;

  constructor(private http: HttpClient) {}

  createApplication(applicationData: any): Observable<Application> {
    return this.http.post<Application>(`${this.apiUrl}/submit`, applicationData);
  }

  getApplicationByStatus(status: string): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/${status}`);
  }

  getApplicationById(applicationId: number): Observable<Application> {
    return this.http.get<Application>(`${this.apiUrl}/${applicationId}`);
  }

  getMyApplications(): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/my-applications`);
  }

  /**
   * Checks if the current user has an active application.
   * An active application has status: PENDING, APPROVED, or AWAITING_ALLOCATION.
   * @returns Observable<boolean> true if user has an active application
   */
  hasActiveApplication(): Observable<boolean> {
    return this.getMyApplications().pipe(
      map((applications) => {
        const activeStatuses = ['PENDING', 'APPROVED', 'AWAITING_ALLOCATION'];
        return applications.some((app) => activeStatuses.includes(app.status));
      }),
      catchError(() => of(false))
    );
  }

  /**
   * Gets the active application for the current user.
   * @returns Observable<Application | null> the active application or null
   */
  getActiveApplication(): Observable<Application | null> {
    return this.getMyApplications().pipe(
      map((applications) => {
        const activeStatuses = ['PENDING', 'APPROVED', 'AWAITING_ALLOCATION'];
        return applications.find((app) => activeStatuses.includes(app.status)) || null;
      }),
      catchError(() => of(null))
    );
  }
}
