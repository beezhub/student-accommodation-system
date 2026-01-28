import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export enum GenderRestriction {
  NONE = 'NONE',
  MALE_ONLY = 'MALE_ONLY',
  FEMALE_ONLY = 'FEMALE_ONLY',
  MIXED = 'MIXED'
}

export interface BuildingAttributes {
  id: number;
  buildingId: number;
  genderRestriction: GenderRestriction;
  hasElevator: boolean;
  wheelchairAccessible: boolean;
  desirabilityScore: number; // 0-100
  accessType: string; // e.g., 'RFID', 'KEY'
  amenities: string[]; // e.g., ['GYM', 'LAUNDRY']
  updatedAt: string;
}

export interface RoomAttributes {
  id: number;
  roomId: number;
  roomNumber: string;
  genderRestriction: GenderRestriction;
  wheelchairAccessible: boolean;
  desirabilityScore: number;
  capacity: number;
  floor: number;
}

@Injectable({
  providedIn: 'root'
})
export class BuildingAttributesService {
  private apiUrl = `${environment.apiUrl}/building-attributes`;

  constructor(private http: HttpClient) {}

  getBuildingAttributes(buildingId: number): Observable<BuildingAttributes> {
    return this.http.get<BuildingAttributes>(`${this.apiUrl}/building/${buildingId}`);
  }

  saveBuildingAttributes(buildingId: number, attributes: Partial<BuildingAttributes>): Observable<BuildingAttributes> {
    return this.http.post<BuildingAttributes>(`${this.apiUrl}/building/${buildingId}`, attributes);
  }

  getRoomAttributes(buildingId: number): Observable<RoomAttributes[]> {
    return this.http.get<RoomAttributes[]>(`${this.apiUrl}/building/${buildingId}/rooms`);
  }

  saveRoomAttributes(roomId: number, attributes: Partial<RoomAttributes>): Observable<RoomAttributes> {
    return this.http.put<RoomAttributes>(`${this.apiUrl}/room/${roomId}`, attributes);
  }
}
