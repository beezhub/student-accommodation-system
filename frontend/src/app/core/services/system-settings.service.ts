import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class SystemSettingsService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/admin/system-settings`;
  private publicApiUrl = `${environment.apiUrl}/settings`;

  getSettings(): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(this.apiUrl);
  }

  updateSetting(key: string, value: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${key}`, { value });
  }

  deleteSetting(key: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${key}`);
  }

  // isAutoAllocationEnabled() lived here and read the `auto_allocation_enabled` setting. Auto-
  // allocation is gated by the FF4j `auto-allocation` flag alone now, and that row is deleted, so
  // this would have returned false forever (GH-207). Read the flag through FeatureFlagService.

  /**
   * Fetches a single setting value via the public (authenticated) endpoint.
   * Returns null if the setting does not exist or the request fails.
   */
  getPublicSetting(key: string): Observable<string | null> {
    return this.http.get<{ key: string; value?: string }>(`${this.publicApiUrl}/${key}`).pipe(
      map((response) => response.value ?? null),
      catchError(() => of(null))
    );
  }
}
