import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface OwnerBuildingSummary {
  id: number;
  name: string;
  address: string;
  totalRooms: number;
  occupiedRooms: number;
}

@Injectable({ providedIn: 'root' })
export class OwnerPortalService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/owner`;

  getMyBuildings(): Observable<OwnerBuildingSummary[]> {
    return this.http.get<OwnerBuildingSummary[]>(`${this.apiUrl}/buildings`);
  }
}
