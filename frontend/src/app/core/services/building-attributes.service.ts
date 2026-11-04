import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * The part of a building's attributes this app edits. The backend's gender and desirability fields
 * are deprecated: gender is managed on rooms (room-attributes.service.ts).
 */
export interface BuildingAttributes {
  buildingId: number;
  /** A JSON list of text, such as `["Ramp access"]`; empty until a feature is saved. */
  accessibilityFeatures?: string | null;
}

/** The accessibility dialog shows its own message on failure, so the interceptor's would repeat it. */
const SILENT = new HttpHeaders({ 'X-Silent-Error': 'true' });

@Injectable({
  providedIn: 'root',
})
export class BuildingAttributesService {
  private apiUrl = `${environment.apiUrl}/admin/buildings`;

  private http = inject(HttpClient);

  getBuildingAttributes(buildingId: number): Observable<BuildingAttributes> {
    return this.http.get<BuildingAttributes>(`${this.apiUrl}/${buildingId}/attributes`, {
      headers: SILENT,
    });
  }

  /** The backend leaves any field not sent as it is, so this touches only the features. */
  saveAccessibilityFeatures(
    buildingId: number,
    features: string[]
  ): Observable<BuildingAttributes> {
    return this.http.put<BuildingAttributes>(
      `${this.apiUrl}/${buildingId}/attributes`,
      { buildingId, accessibilityFeatures: JSON.stringify(features) },
      { headers: SILENT }
    );
  }
}
