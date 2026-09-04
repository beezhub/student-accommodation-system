import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type GenderRestriction = 'ANY' | 'MALE_ONLY' | 'FEMALE_ONLY';

export interface RoomAttributesResponse {
  id?: number;
  roomId: number;
  roomNumber?: string;
  buildingId?: number;
  buildingName?: string;
  accessibilityCompliant?: boolean;
  genderRestriction?: GenderRestriction;
  minYearOfStudy?: number;
  maxYearOfStudy?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface RoomAttributesRequest {
  roomId: number;
  accessibilityCompliant?: boolean;
  genderRestriction?: GenderRestriction;
  minYearOfStudy?: number;
  maxYearOfStudy?: number;
}

@Injectable({
  providedIn: 'root',
})
export class RoomAttributesService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/admin/room-attributes`;

  getAttributesByRoomId(roomId: number): Observable<RoomAttributesResponse> {
    return this.http.get<RoomAttributesResponse>(`${this.apiUrl}/room/${roomId}`);
  }

  getAttributesByBuildingId(buildingId: number): Observable<RoomAttributesResponse[]> {
    return this.http.get<RoomAttributesResponse[]>(`${this.apiUrl}/building/${buildingId}`);
  }

  saveAttributes(request: RoomAttributesRequest): Observable<RoomAttributesResponse> {
    return this.http.post<RoomAttributesResponse>(this.apiUrl, request);
  }

  bulkSaveAttributes(requests: RoomAttributesRequest[]): Observable<RoomAttributesResponse[]> {
    return this.http.post<RoomAttributesResponse[]>(`${this.apiUrl}/bulk`, requests);
  }

  deleteAttributes(roomId: number): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/room/${roomId}`);
  }
}
