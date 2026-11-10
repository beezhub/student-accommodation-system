import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
export { PageResponse } from '../models/page-response.model';
import { PageResponse } from '../models/page-response.model';

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
  expectedCheckInDate?: string;
  checkInDate?: string;
  /** Planned move-in, 'YYYY-MM-DD' (GH-496). */
  stayStartDate?: string;
  /** Planned check-out, 'YYYY-MM-DD': the bed is free again from this date. */
  stayEndDate?: string;
  /** When the offer was accepted (GH-496). */
  acceptedAt?: string;
  /** When the student was asked "Still coming?" before move-in. */
  reconfirmRequestedAt?: string;
  /** When the student confirmed they are still coming. */
  reconfirmedAt?: string;
  declineReason?: DeclineReason;
  declineReasonText?: string;
  /**
   * Times this allocation's application has already been declined. Sent on single-allocation
   * reads so a student can be warned before declining again; absent on list responses.
   */
  previousDeclineCount?: number;
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
    institutionId?: number;
    intakeId?: number;
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
    if (filters.institutionId)
      params = params.set('institutionId', filters.institutionId.toString());
    if (filters.intakeId) params = params.set('intakeId', filters.intakeId.toString());

    return this.http.get<PageResponse<RoomAllocationResponse>>(`${this.apiUrl}/search`, {
      params,
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
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
    return this.http.post<RoomAllocationResponse>(this.apiUrl, allocation, {
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
  }

  allocateByApplication(request: AllocateByApplicationRequest): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(
      `${this.apiUrl}/allocate-by-application`,
      request,
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  updateAllocation(
    id: number,
    allocation: RoomAllocationRequest
  ): Observable<RoomAllocationResponse> {
    return this.http.put<RoomAllocationResponse>(`${this.apiUrl}/${id}`, allocation);
  }

  endAllocation(id: number): Observable<RoomAllocationResponse> {
    return this.http.patch<RoomAllocationResponse>(
      `${this.apiUrl}/${id}/end`,
      {},
      {
        headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
      }
    );
  }

  deleteAllocation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, {
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
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

  /** The student confirms they are still coming before move-in, so the place keeps its bed (GH-496). */
  reconfirm(id: number): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(`${this.apiUrl}/${id}/reconfirm`, {});
  }

  /**
   * Silent because the dashboard renders its own message for both the max-declines 403 and the
   * generic failure. Without this the interceptor stacked a second, vaguer snackbar on top of it
   * (GH-150). Matches getActiveAllocationByStudentId above.
   */
  declineOffer(id: number, request: DeclineOfferRequest): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(`${this.apiUrl}/${id}/decline`, request, {
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
  }

  /**
   * Moves an outstanding offer's response deadline, so a student who missed it can still accept.
   *
   * <p>Admin only, and recorded against the allocation with the reason given. Refused once the
   * expiry sweep has flipped the offer to EXPIRED, because the room has been released by then
   * (GH-198).
   *
   * <p>Silent because the caller decides what to say: the backend's refusals are specific and
   * worth showing verbatim rather than behind a generic snackbar.
   */
  extendOffer(
    id: number,
    newExpiryDate: string,
    reason: string
  ): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(
      `${this.apiUrl}/${id}/extend-offer`,
      { newExpiryDate, reason },
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  /**
   * Confirms a student's arrival.
   *
   * <p>Refused with 409 once the check-in deadline has passed. Pass overrideDeadline to check the
   * student in anyway; the backend records that against the allocation's audit trail (GH-197).
   *
   * <p>Silent because the caller decides what to say: a passed deadline is a question to put to
   * the admin, not an error to flash at them.
   */
  checkIn(id: number, overrideDeadline = false): Observable<RoomAllocationResponse> {
    const params = overrideDeadline
      ? new HttpParams().set('overrideDeadline', 'true')
      : new HttpParams();
    return this.http.post<RoomAllocationResponse>(
      `${this.apiUrl}/${id}/check-in`,
      {},
      { params, headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  checkOut(id: number): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(
      `${this.apiUrl}/${id}/check-out`,
      {},
      {
        headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
      }
    );
  }
}
