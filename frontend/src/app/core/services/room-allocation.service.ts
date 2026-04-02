import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export enum DeclineReason {
  WRONG_LOCATION = 'WRONG_LOCATION',
  TOO_EXPENSIVE = 'TOO_EXPENSIVE',
  FOUND_ALTERNATIVE = 'FOUND_ALTERNATIVE',
  ACCESSIBILITY_ISSUES = 'ACCESSIBILITY_ISSUES',
  PERSONAL_REASONS = 'PERSONAL_REASONS',
  OTHER = 'OTHER',
}

export interface DeclineOfferRequest {
  declineReason: DeclineReason;
  declineReasonText?: string;
}

export interface RoomAllocationRequest {
  studentId: number;
  roomId: number;
  applicationId?: number;
  allocationDate?: string;
  endDate?: string;
}

export interface RoomAllocationResponse {
  id: number;
  studentId: number;
  studentFirstName: string;
  studentLastName: string;
  studentEmail: string;
  roomId: number;
  roomNumber: string;
  buildingName: string;
  applicationId?: number;
  allocationDate: string;
  endDate?: string;
  status: string;
  offerExpiryDate?: string;
  checkInExpiryDate?: string;
  declineReason?: DeclineReason;
  declineReasonText?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RoomAvailabilityResponse {
  roomId: number;
  roomNumber: string;
  buildingName: string;
  capacity: number;
  currentOccupancy: number;
  availableSpots: number;
  isAvailable: boolean;
  status: string;
}

export interface AllocationEligibilityResponse {
  studentId: number;
  isEligible: boolean;
  reason?: string;
  hasApprovedApplication: boolean;
  hasActiveAllocation: boolean;
}

export interface AllocateByApplicationRequest {
  applicationId: number;
  roomId: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root',
})
export class RoomAllocationService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/room-allocation`;

  getAllAllocations(
    page: number = 0,
    size: number = 20
  ): Observable<PageResponse<RoomAllocationResponse>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<RoomAllocationResponse>>(this.apiUrl, { params });
  }

  getAllAllocationsNoPagination(): Observable<RoomAllocationResponse[]> {
    return this.http.get<RoomAllocationResponse[]>(`${this.apiUrl}/all`);
  }

  searchAllocations(filters: {
    studentId?: number;
    roomId?: number;
    buildingId?: number;
    fromDate?: string;
    toDate?: string;
    studentName?: string;
    roomNumber?: string;
    status?: string;
    page?: number;
    size?: number;
  }): Observable<PageResponse<RoomAllocationResponse>> {
    let params = new HttpParams()
      .set('page', (filters.page || 0).toString())
      .set('size', (filters.size || 20).toString());

    if (filters.studentId) params = params.set('studentId', filters.studentId.toString());
    if (filters.roomId) params = params.set('roomId', filters.roomId.toString());
    if (filters.buildingId) params = params.set('buildingId', filters.buildingId.toString());
    if (filters.fromDate) params = params.set('fromDate', filters.fromDate);
    if (filters.toDate) params = params.set('toDate', filters.toDate);
    if (filters.studentName) params = params.set('studentName', filters.studentName);
    if (filters.roomNumber) params = params.set('roomNumber', filters.roomNumber);
    if (filters.status) params = params.set('status', filters.status);

    return this.http.get<PageResponse<RoomAllocationResponse>>(`${this.apiUrl}/search`, { params });
  }

  getAllocationsByStatus(
    status: string,
    page: number = 0,
    size: number = 20
  ): Observable<PageResponse<RoomAllocationResponse>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<RoomAllocationResponse>>(`${this.apiUrl}/status/${status}`, {
      params,
    });
  }

  getAllocationById(id: number): Observable<RoomAllocationResponse> {
    return this.http.get<RoomAllocationResponse>(`${this.apiUrl}/${id}`);
  }

  getActiveAllocationByStudentId(studentId: number): Observable<RoomAllocationResponse> {
    return this.http.get<RoomAllocationResponse>(`${this.apiUrl}/student/${studentId}/active`, {
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
  }

  createAllocation(allocation: RoomAllocationRequest): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(this.apiUrl, allocation);
  }

  allocateByApplication(request: AllocateByApplicationRequest): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(
      `${this.apiUrl}/allocate-by-application`,
      request
    );
  }

  updateAllocation(
    id: number,
    allocation: RoomAllocationRequest
  ): Observable<RoomAllocationResponse> {
    return this.http.put<RoomAllocationResponse>(`${this.apiUrl}/${id}`, allocation);
  }

  endAllocation(id: number): Observable<RoomAllocationResponse> {
    return this.http.patch<RoomAllocationResponse>(`${this.apiUrl}/${id}/end`, {});
  }

  deleteAllocation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getRoomAvailability(roomId: number): Observable<RoomAvailabilityResponse> {
    return this.http.get<RoomAvailabilityResponse>(`${this.apiUrl}/room/${roomId}/availability`);
  }

  isRoomAvailable(roomId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/room/${roomId}/is-available`);
  }

  checkStudentEligibility(studentId: number): Observable<AllocationEligibilityResponse> {
    return this.http.get<AllocationEligibilityResponse>(
      `${this.apiUrl}/student/${studentId}/eligibility`
    );
  }

  acceptOffer(id: number, expectedCheckInDate: string): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(`${this.apiUrl}/${id}/accept`, {
      expectedCheckInDate,
    });
  }

  declineOffer(id: number, request: DeclineOfferRequest): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(`${this.apiUrl}/${id}/decline`, request);
  }

  checkIn(id: number): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(`${this.apiUrl}/${id}/check-in`, {});
  }

  checkOut(id: number): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(`${this.apiUrl}/${id}/check-out`, {});
  }
}
