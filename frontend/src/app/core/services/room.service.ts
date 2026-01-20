import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RoomRequest {
  roomNumber: string;
  capacity: number;
  currentOccupancy?: number;
  status: string;
  buildingId: number;
}

export interface RoomResponse {
  id: number;
  roomNumber: string;
  capacity: number;
  currentOccupancy: number;
  status: string;
  buildingId: number;
  buildingName: string;
  createdAt: string;
  updatedAt: string;
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
export class RoomService {
  private apiUrl = `${environment.apiUrl}/room`;

  constructor(private http: HttpClient) {}

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

    return this.http.get<PageResponse<RoomResponse>>(`${this.apiUrl}/search`, { params });
  }

  getRoomById(id: number): Observable<RoomResponse> {
    return this.http.get<RoomResponse>(`${this.apiUrl}/${id}`);
  }

  createRoom(room: RoomRequest): Observable<RoomResponse> {
    return this.http.post<RoomResponse>(this.apiUrl, room);
  }

  updateRoom(id: number, room: RoomRequest): Observable<RoomResponse> {
    return this.http.put<RoomResponse>(`${this.apiUrl}/${id}`, room);
  }

  deleteRoom(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
