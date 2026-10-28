import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BuildingPreferenceResponse } from './application.service';
export { PageResponse } from '../models/page-response.model';
import { PageResponse } from '../models/page-response.model';

export interface CreateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  userRole: string; // Role must be PLATFORM_ADMIN, BUILDING_OWNER, or BUILDING_STAFF
  tenantId?: number;
}

export interface AdminUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  userRole: string;
  mustChangePassword?: boolean;
}

export interface ApplicationResponse {
  id: number;
  applicationCode: string;
  applicationDate: string;
  status: string;
  remarks?: string;
  studentId: number;
  studentFirstName: string;
  studentLastName: string;
  studentEmail: string;
  // Allocation & Review Details
  preferredBuildingName?: string;
  documentCompletionPercent?: number;
  approvedDocuments?: number;
  totalRequiredDocuments?: number;
  allocationStatus?: string; // PENDING_ALLOCATION, ALLOCATED, REJECTED
  allocationScore?: number; // Priority score for allocation (0-100)
  // Preference fields for quick view
  requiresAccessibility?: boolean;
  specialRequirements?: string;
  courseName?: string;
  preferredBuildings?: BuildingPreferenceResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface DocumentResponse {
  id: number;
  documentName: string;
  documentPath: string;
  documentStatus: string;
  documentTypeName: string;
  studentId: number;
  studentFirstName: string;
  studentLastName: string;
  studentEmail: string;
  applicationStatus?: string;
  applicationId?: number;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardStats {
  // Application stats
  totalApplications: number;
  pendingApplications: number;
  approvedApplications: number;
  rejectedApplications: number;
  inReviewApplications: number;
  allocatedApplications: number;
  // Document stats
  totalDocuments: number;
  pendingDocuments: number;
  approvedDocuments: number;
  rejectedDocuments: number;
  // User stats
  totalStudents: number;
  totalAdmins: number;
  // Room stats
  totalRooms: number;
  availableRooms: number;
  occupiedRooms: number;
  fullRooms: number;
  maintenanceRooms: number;
  // Building stats
  totalBuildings: number;
  // Occupancy summary
  totalCapacity: number;
  totalOccupancy: number;
  overallOccupancyRate: number;
}

export interface BuildingOccupancy {
  buildingId: number;
  buildingName: string;
  roomCount: number;
  totalCapacity: number;
  currentOccupancy: number;
  occupancyRate: number;
}

export interface OccupancyStats {
  totalBuildings: number;
  totalRooms: number;
  totalCapacity: number;
  totalOccupancy: number;
  overallOccupancyRate: number;
  buildingOccupancies: BuildingOccupancy[];
}

export interface BulkStatusUpdateRequest {
  ids: number[];
  status: string;
  remarks?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private http = inject(HttpClient);

  private userApiUrl = `${environment.apiUrl}/admin/users`;
  private applicationApiUrl = `${environment.apiUrl}/applications`;
  private documentApiUrl = `${environment.apiUrl}/documents`;

  // User Management
  createUser(userData: CreateUserRequest): Observable<AdminUser> {
    return this.http.post<AdminUser>(this.userApiUrl, userData);
  }

  /**
   * One page of users. Was unpaged, so the admin screen fetched every account in the deployment
   * on each visit and the response grew without limit (GH-157).
   */
  getAllUsers(page = 0, size = 25): Observable<PageResponse<AdminUser>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AdminUser>>(this.userApiUrl, { params });
  }

  getUsersByRole(role: string, page = 0, size = 25): Observable<PageResponse<AdminUser>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AdminUser>>(`${this.userApiUrl}/role/${role}`, { params });
  }

  deleteUser(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.userApiUrl}/${userId}`);
  }

  // Application Management
  getAllApplications(
    page: number = 0,
    size: number = 20,
    sort: string = 'applicationDate'
  ): Observable<PageResponse<ApplicationResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    return this.http.get<PageResponse<ApplicationResponse>>(this.applicationApiUrl, { params });
  }

  searchApplications(filters: {
    applicationCode?: string;
    studentName?: string;
    status?: string;
    allocationStatus?: string;
    preferredBuilding?: string;
    startDate?: string;
    endDate?: string;
    page?: number;
    size?: number;
  }): Observable<PageResponse<ApplicationResponse>> {
    let params = new HttpParams()
      .set('page', (filters.page || 0).toString())
      .set('size', (filters.size || 20).toString());

    if (filters.applicationCode) params = params.set('applicationCode', filters.applicationCode);
    if (filters.studentName) params = params.set('studentName', filters.studentName);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.allocationStatus) params = params.set('allocationStatus', filters.allocationStatus);
    if (filters.preferredBuilding)
      params = params.set('preferredBuilding', filters.preferredBuilding);
    if (filters.startDate) params = params.set('startDate', filters.startDate);
    if (filters.endDate) params = params.set('endDate', filters.endDate);

    return this.http.get<PageResponse<ApplicationResponse>>(`${this.applicationApiUrl}/search`, {
      params,
    });
  }

  getApplicationsByStatus(status: string): Observable<ApplicationResponse[]> {
    return this.http.get<ApplicationResponse[]>(`${this.applicationApiUrl}/status/${status}`);
  }

  updateApplicationStatus(
    applicationId: number,
    status: string,
    remarks?: string
  ): Observable<ApplicationResponse> {
    return this.http.patch<ApplicationResponse>(
      `${this.applicationApiUrl}/${applicationId}/status`,
      {
        status,
        remarks,
      },
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  // Document Management
  searchDocuments(filters: {
    studentName?: string;
    documentTypeId?: number;
    status?: string;
    page?: number;
    size?: number;
  }): Observable<PageResponse<DocumentResponse>> {
    let params = new HttpParams()
      .set('page', (filters.page || 0).toString())
      .set('size', (filters.size || 20).toString());

    if (filters.studentName) params = params.set('studentName', filters.studentName);
    if (filters.documentTypeId)
      params = params.set('documentTypeId', filters.documentTypeId.toString());
    if (filters.status) params = params.set('status', filters.status);

    return this.http.get<PageResponse<DocumentResponse>>(`${this.documentApiUrl}/search`, {
      params,
    });
  }

  updateDocumentStatus(documentId: number, status: string): Observable<DocumentResponse> {
    const params = new HttpParams().set('status', status);
    return this.http.put<DocumentResponse>(
      `${this.documentApiUrl}/update-status/${documentId}`,
      null,
      { params }
    );
  }

  downloadDocument(documentId: number): Observable<Blob> {
    return this.http.get(`${this.documentApiUrl}/download/${documentId}`, {
      responseType: 'blob',
    });
  }

  // Statistics
  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${environment.apiUrl}/admin/stats`);
  }

  getOccupancyStats(): Observable<OccupancyStats> {
    return this.http.get<OccupancyStats>(`${environment.apiUrl}/admin/stats/occupancy`);
  }
}
