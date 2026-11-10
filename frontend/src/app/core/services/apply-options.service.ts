import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CompanyApplyOptions } from '../models/apply-options.model';
import { BuildingResponse } from './building.service';

/** What the application form offers a student (GH-495): `/api/apply`. */
@Injectable({ providedIn: 'root' })
export class ApplyOptionsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/apply`;

  /** Buildings of any company that can take the institution; empty for the profile's one. */
  getBuildings(institutionId?: number | null): Observable<BuildingResponse[]> {
    return this.http.get<BuildingResponse[]>(`${this.apiUrl}/buildings`, {
      params: this.institutionParam(institutionId),
    });
  }

  /** The building's company: open intakes, the student's applications there, missing documents. */
  getOptions(buildingId: number, institutionId?: number | null): Observable<CompanyApplyOptions> {
    return this.http.get<CompanyApplyOptions>(`${this.apiUrl}/buildings/${buildingId}/intakes`, {
      params: this.institutionParam(institutionId),
    });
  }

  /** Companies with an intake open today that the student has not applied for yet. */
  getOpenWindows(): Observable<CompanyApplyOptions[]> {
    return this.http.get<CompanyApplyOptions[]>(`${this.apiUrl}/open-windows`, {
      headers: { 'X-Silent-Error': 'true' },
    });
  }

  private institutionParam(institutionId?: number | null): HttpParams {
    return institutionId
      ? new HttpParams().set('institutionId', String(institutionId))
      : new HttpParams();
  }
}
