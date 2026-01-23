import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, catchError, of, forkJoin } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Application } from '../models/application.model';

export interface StudentProfile {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  studentNumber: string;
  dateOfBirth: string;
  gender: string;
  specialRequirements: string;
  institutionId: number;
  institutionName?: string;
  yearOfStudyId: number;
  yearOfStudyName?: string;
  registrationDate: string;
}

export interface ApplicationDocument {
  id: number;
  documentName: string;
  documentPath: string;
  documentStatus: string;
  documentTypeId: number;
  documentTypeName: string;
  isRequired: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentTypeChecklist {
  id: number;
  typeName: string;
  isRequired: boolean;
  status: 'APPROVED' | 'PENDING' | 'REJECTED' | 'NOT_UPLOADED';
  documentId?: number;
}

export interface ApplicationFullDetails {
  application: Application;
  student: StudentProfile;
  documents: ApplicationDocument[];
  documentChecklist: DocumentTypeChecklist[];
  requiredDocumentsApproved: number;
  requiredDocumentsTotal: number;
  allRequiredApproved: boolean;
}

export interface ReviewProgressRequest {
  applicationId: number;
  documentUpdates: { documentId: number; status: string }[];
}

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

  /**
   * Gets full application details including student profile, documents, and checklist.
   * @param applicationId The application ID
   * @returns Observable<ApplicationFullDetails> Full application details
   */
  getApplicationFullDetails(applicationId: number): Observable<ApplicationFullDetails> {
    return this.http.get<ApplicationFullDetails>(`${this.apiUrl}/${applicationId}/full-details`);
  }

  /**
   * Saves review progress (document status changes) without completing the review.
   * @param request The review progress request
   * @returns Observable<{ success: boolean }>
   */
  saveReviewProgress(request: ReviewProgressRequest): Observable<{ success: boolean }> {
    return this.http.post<{ success: boolean }>(`${this.apiUrl}/review-progress`, request);
  }

  /**
   * Moves an application to the allocation phase.
   * @param applicationId The application ID
   * @returns Observable<Application> Updated application
   */
  moveToAllocationPhase(applicationId: number): Observable<Application> {
    return this.http.patch<Application>(`${this.apiUrl}/${applicationId}/move-to-allocation`, {});
  }

  /**
   * Gets documents for a specific application.
   * @param applicationId The application ID
   * @returns Observable<ApplicationDocument[]> List of documents
   */
  getApplicationDocuments(applicationId: number): Observable<ApplicationDocument[]> {
    return this.http.get<ApplicationDocument[]>(`${this.apiUrl}/${applicationId}/documents`);
  }
}
