import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

import { Application } from '../models/application.model';
import { ApplicationSubmission } from '../models/apply-options.model';
import { ApplicationStatus } from '../models/status.model';

export interface BuildingPreferenceResponse {
  buildingId: number;
  buildingName: string;
  preferenceOrder: number;
}

export interface StudentProfile {
  id: number;
  studentNumber: string;
  firstName: string;
  lastName: string;
  fullName?: string;
  email: string;
  phoneNumber?: string;
  gender?: string;
  specialRequirements?: string;
  institutionName?: string;
  yearOfStudy?: string;
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

// Matches backend ApplicationFullDetailsResponse
export interface ApplicationFullDetails {
  // Application data (flat)
  id: number;
  applicationCode: string;
  status: string;
  applicationDate: string;
  createdAt: string;
  updatedAt: string;
  remarks?: string;
  // Preferences
  requiresAccessibility?: boolean;
  specialRequirements?: string;
  institutionName?: string;
  courseName?: string;
  preferredBuildingId?: number;
  preferredBuildingName?: string;
  preferredBuildings?: BuildingPreferenceResponse[];
  allocationMethod?: string;
  // Nested objects
  studentProfile: StudentProfile;
  documents: DocumentWithComments[];
  requiredDocumentsChecklist: RequiredDocumentsChecklist;
  allocationEligibility: AllocationEligibility;
}

export interface DocumentWithComments {
  id: number;
  fileName: string;
  type: string;
  status: string;
  uploadedAt: string;
  documentTypeId: number;
  documentTypeName: string;
  comments: DocumentComment[];
}

export interface DocumentComment {
  id: number;
  adminName: string;
  text: string;
  createdAt: string;
}

export interface RequiredDocumentsChecklist {
  applicationId: number;
  totalRequired: number;
  approved: number;
  pending: number;
  rejected: number;
  notUploaded: number;
  documents: DocumentChecklistItem[];
  allApproved: boolean;
}

export interface DocumentChecklistItem {
  documentTypeId: number;
  type: string;
  status: 'APPROVED' | 'PENDING' | 'REJECTED' | 'NOT_UPLOADED';
  uploadedAt?: string;
  documentId?: number;
  documentName?: string;
  isRequired: boolean;
}

export interface AllocationEligibility {
  eligible: boolean;
  applicationApproved: boolean;
  allDocumentsApproved: boolean;
  reason?: string;
}

export interface ReviewProgressRequest {
  applicationId: number;
  documentUpdates: { documentId: number; newStatus: string }[];
}

@Injectable({
  providedIn: 'root',
})
export class ApplicationService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/applications`;

  /** Submits the form; returns the applications created, one per company (GH-495). */
  createApplication(applicationData: Partial<ApplicationSubmission>): Observable<Application[]> {
    return this.http.post<Application[]>(`${this.apiUrl}/submit`, applicationData);
  }

  getApplicationByStatus(status: string): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/status/${status}`);
  }

  getApplicationById(applicationId: number): Observable<Application> {
    return this.http.get<Application>(`${this.apiUrl}/${applicationId}`, {
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
  }

  getApplicationsByStudentId(studentId: number): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/student/${studentId}`);
  }

  deleteApplication(applicationId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${applicationId}`);
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
        const activeStatuses = [
          ApplicationStatus.PENDING,
          ApplicationStatus.APPROVED,
          ApplicationStatus.AWAITING_ALLOCATION,
          ApplicationStatus.UNDER_REVIEW,
          ApplicationStatus.CHANGES_REQUESTED,
          ApplicationStatus.ALLOCATED,
        ];
        return applications.some((app) => activeStatuses.includes(app.status as ApplicationStatus));
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
        const activeStatuses = [
          ApplicationStatus.PENDING,
          ApplicationStatus.APPROVED,
          ApplicationStatus.AWAITING_ALLOCATION,
          ApplicationStatus.UNDER_REVIEW,
          ApplicationStatus.CHANGES_REQUESTED,
          ApplicationStatus.ALLOCATED,
        ];
        return (
          applications.find((app) => activeStatuses.includes(app.status as ApplicationStatus)) ||
          null
        );
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
    return this.http.get<ApplicationFullDetails>(`${this.apiUrl}/${applicationId}/full-details`, {
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
  }

  /**
   * Saves review progress (document status changes) without completing the review.
   * @param request The review progress request
   * @returns Observable<{ success: boolean }>
   */
  saveReviewProgress(request: ReviewProgressRequest): Observable<{ success: boolean }> {
    return this.http.post<{ success: boolean }>(
      `${this.apiUrl}/${request.applicationId}/save-review-progress`,
      request,
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  /**
   * Moves an application to the allocation phase.
   * @param applicationId The application ID
   * @returns Observable<Application> Updated application
   */
  moveToAllocationPhase(applicationId: number): Observable<Application> {
    return this.http.post<Application>(
      `${this.apiUrl}/${applicationId}/move-to-allocation`,
      {},
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  /**
   * Gets documents for a specific application.
   * Delegates to the full-details endpoint and extracts the documents array,
   * since no dedicated /documents endpoint exists on the backend.
   * @param applicationId The application ID
   * @returns Observable<ApplicationDocument[]> List of documents
   */
  getApplicationDocuments(applicationId: number): Observable<ApplicationDocument[]> {
    return this.getApplicationFullDetails(applicationId).pipe(
      map((details) =>
        details.documents.map((doc) => ({
          id: doc.id,
          documentName: doc.fileName,
          documentPath: '',
          documentStatus: doc.status,
          documentTypeId: doc.documentTypeId,
          documentTypeName: doc.documentTypeName,
          isRequired: false,
          createdAt: doc.uploadedAt,
          updatedAt: doc.uploadedAt,
        }))
      )
    );
  }

  /**
   * Marks an application as under review.
   * @param applicationId The application ID
   */
  markUnderReview(applicationId: number): Observable<Application> {
    return this.http.post<Application>(`${this.apiUrl}/${applicationId}/mark-under-review`, {});
  }

  /**
   * Requests changes for an application.
   * @param applicationId The application ID
   * @param reason The reason for requesting changes
   */
  requestChanges(applicationId: number, reason: string): Observable<Application> {
    return this.http.post<Application>(`${this.apiUrl}/${applicationId}/request-changes`, {
      reason,
    });
  }

  /**
   * Sends an application with changes requested back to the review queue. Student only.
   * @param applicationId The application ID
   */
  resubmitApplication(applicationId: number): Observable<Application> {
    return this.http.post<Application>(`${this.apiUrl}/${applicationId}/resubmit`, {});
  }
}
