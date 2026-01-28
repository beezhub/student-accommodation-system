import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SystemSettings {
  autoAllocationEnabled: boolean;
  // Add other settings here as needed
}

@Injectable({
  providedIn: 'root',
})
export class SystemSettingsService {
  private apiUrl = `${environment.apiUrl}/admin/settings`;

  constructor(private http: HttpClient) {}

  getSettings(): Observable<SystemSettings> {
    return this.http.get<SystemSettings>(this.apiUrl);
  }

  // Helper to quickly check a specific setting
  isAutoAllocationEnabled(): Observable<boolean> {
    // For now returning true mock if backend not ready, or map from getSettings
    // return this.getSettings().pipe(map(s => s.autoAllocationEnabled));
    // Mock for development until backend endpoint exists:
    return of(true); 
  }
}
