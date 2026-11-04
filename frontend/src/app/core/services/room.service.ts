import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
export { PageResponse } from '../models/page-response.model';
import { PageResponse } from '../models/page-response.model';

export interface RoomRequest {
  roomNumber: string;
  capacity: number;
  currentOccupancy?: number;
  status: string;
  monthlyRate?: number | null;
  buildingId: number;
}

export interface RoomResponse {
  id: number;
  roomNumber: string;
  capacity: number;
  currentOccupancy: number;
  status: string;
  monthlyRate?: number | null;
  buildingId: number;
  buildingName: string;
  accessibilityCompliant?: boolean;
  genderRestriction?: 'ANY' | 'MALE_ONLY' | 'FEMALE_ONLY';
  minYearOfStudy?: number;
  maxYearOfStudy?: number;
  createdAt: string;
  updatedAt: string;
}

export interface EligibleRoomResponse {
  roomId: number;
  roomNumber: string;
  capacity: number;
  currentOccupancy: number;
  availableSpots: number;
  status: string;
  buildingId: number;
  buildingName: string;
  buildingAddress?: string;
  genderRestriction?: string;
  accessibilityCompliant?: boolean;
  minYearOfStudy?: number;
  maxYearOfStudy?: number;
}

// Callers of the write methods below show their own failure message.
const SILENT = { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) };

@Injectable({
  providedIn: 'root',
})
export class RoomService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/room`;

  getAllRooms(page: number = 0, size: number = 20): Observable<PageResponse<RoomResponse>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<RoomResponse>>(this.apiUrl, { params });
  }

  getAllRoomsNoPagination(): Observable<RoomResponse[]> {
    return this.http.get<RoomResponse[]>(`${this.apiUrl}/all`);
  }

  searchRooms(filters: {
    roomNumber?: string;
    buildingId?: number;
    minCapacity?: number;
    maxCapacity?: number;
    onlyAccessible?: boolean;
    onlyAvailable?: boolean;
    page?: number;
    size?: number;
  }): Observable<PageResponse<RoomResponse>> {
    let params = new HttpParams()
      .set('page', (filters.page || 0).toString())
      .set('size', (filters.size || 20).toString());

    if (filters.roomNumber) params = params.set('roomNumber', filters.roomNumber);
    if (filters.buildingId) params = params.set('buildingId', filters.buildingId.toString());
    if (filters.minCapacity) params = params.set('minCapacity', filters.minCapacity.toString());
    if (filters.maxCapacity) params = params.set('maxCapacity', filters.maxCapacity.toString());
    if (filters.onlyAccessible) params = params.set('accessibility', 'true');
    if (filters.onlyAvailable) params = params.set('onlyAvailable', 'true');

    return this.http.get<PageResponse<RoomResponse>>(`${this.apiUrl}/search`, {
      params,
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
  }

  getRoomById(id: number): Observable<RoomResponse> {
    return this.http.get<RoomResponse>(`${this.apiUrl}/${id}`);
  }

  createRoom(room: RoomRequest): Observable<RoomResponse> {
    return this.http.post<RoomResponse>(this.apiUrl, room, SILENT);
  }

  updateRoom(id: number, room: RoomRequest): Observable<RoomResponse> {
    return this.http.put<RoomResponse>(`${this.apiUrl}/${id}`, room, SILENT);
  }

  deleteRoom(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, SILENT);
  }

  createBulkRooms(rooms: RoomRequest[]): Observable<RoomResponse[]> {
    return this.http.post<RoomResponse[]>(`${this.apiUrl}/bulk`, rooms, SILENT);
  }

  /**
   * Get eligible rooms for a student based on their restrictions (gender, year of study, accessibility, institution).
   * @param studentId The student ID to check eligibility for
   * @param buildingId Optional building ID to filter by
   */
  getEligibleRoomsForStudent(
    studentId: number,
    buildingId?: number
  ): Observable<EligibleRoomResponse[]> {
    const url = `${environment.apiUrl}/room-allocation/admin/student/${studentId}/eligible-rooms`;
    let params = new HttpParams();
    if (buildingId) {
      params = params.set('buildingId', buildingId.toString());
    }
    return this.http.get<EligibleRoomResponse[]>(url, {
      params,
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
  }
}
