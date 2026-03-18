import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface OwnerRegistrationSummary {
  id: number;
  fullName: string;
  email: string;
  organisationName: string;
  contactPhone?: string;
  message?: string;
  status: string;
  createdAt: string;
  verifiedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class OwnerRegistrationAdminService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/admin/owner-registrations`;

  getPendingRegistrations(): Observable<OwnerRegistrationSummary[]> {
    return this.http.get<OwnerRegistrationSummary[]>(this.apiUrl);
  }

  approve(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/approve`, {});
  }

  reject(id: number, reason?: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/reject`, { reason });
  }
}
