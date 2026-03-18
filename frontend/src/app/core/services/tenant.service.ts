import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Tenant {
  id: number;
  name: string;
  domain?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface TenantRequest {
  name: string;
  domain?: string;
}

@Injectable({
  providedIn: 'root',
})
export class TenantService {
  private apiUrl = `${environment.apiUrl}/admin/tenants`;

  constructor(private http: HttpClient) {}

  getAllTenants(): Observable<Tenant[]> {
    return this.http.get<Tenant[]>(this.apiUrl);
  }

  getTenantById(id: number): Observable<Tenant> {
    return this.http.get<Tenant>(`${this.apiUrl}/${id}`);
  }

  createTenant(dto: TenantRequest): Observable<Tenant> {
    return this.http.post<Tenant>(this.apiUrl, dto);
  }

  updateTenant(id: number, dto: TenantRequest): Observable<Tenant> {
    return this.http.put<Tenant>(`${this.apiUrl}/${id}`, dto);
  }

  toggleTenantStatus(id: number, isActive: boolean): Observable<Tenant> {
    return this.http.patch<Tenant>(`${this.apiUrl}/${id}/status`, { isActive });
  }
}
