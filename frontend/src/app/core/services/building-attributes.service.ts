import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BuildingAttributes } from '../../model/building-attributes.model';
import { RoomAttributes } from '../../model/room-attributes.model';

@Injectable({
  providedIn: 'root',
})
export class BuildingAttributesService {
  private apiUrl = `${environment.apiUrl}/building-attributes`;

  constructor(private http: HttpClient) {}

  getAttributes(buildingId: number): Observable<BuildingAttributes> {
    return this.http.get<BuildingAttributes>(`${this.apiUrl}/building/${buildingId}`);
  }

  updateAttributes(
    buildingId: number,
    attrs: Partial<BuildingAttributes>
  ): Observable<BuildingAttributes> {
    return this.http.put<BuildingAttributes>(`${this.apiUrl}/building/${buildingId}`, attrs);
  }

  getRoomAttributes(buildingId: number): Observable<RoomAttributes[]> {
    return this.http.get<RoomAttributes[]>(`${this.apiUrl}/building/${buildingId}/rooms`);
  }

  getRoomAttribute(roomId: number): Observable<RoomAttributes> {
    return this.http.get<RoomAttributes>(`${this.apiUrl}/room/${roomId}`);
  }

  updateRoomAttribute(roomId: number, attrs: Partial<RoomAttributes>): Observable<RoomAttributes> {
    return this.http.put<RoomAttributes>(`${this.apiUrl}/room/${roomId}`, attrs);
  }
}
