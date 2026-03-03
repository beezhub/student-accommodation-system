import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class SystemSettingsService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/admin/system-settings`;

  getSettings(): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(this.apiUrl);
  }

  updateSetting(key: string, value: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${key}`, { value });
  }

  isAutoAllocationEnabled(): Observable<boolean> {
    return this.getSettings().pipe(
      map((settings) => settings['auto_allocation_enabled'] === 'true')
    );
  }
}
