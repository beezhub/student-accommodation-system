import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface FeatureFlag {
  uid: string;
  enable: boolean;
  description?: string;
  group?: string;
}

@Injectable({
  providedIn: 'root',
})
export class FeatureFlagService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/admin/features`;

  getAll(): Observable<FeatureFlag[]> {
    return this.http
      .get<Record<string, Partial<FeatureFlag> & { uid: string; enable: boolean }>>(this.apiUrl)
      .pipe(
        map((response) =>
          Object.values(response).map((flag) => ({
            uid: flag.uid,
            enable: flag.enable,
            description: flag.description,
            group: flag.group,
          }))
        )
      );
  }

  enable(uid: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${uid}/enable`, null);
  }

  disable(uid: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${uid}/disable`, null);
  }
}
