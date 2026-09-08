import { Injectable, inject } from '@angular/core';
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

export interface TenantAdminUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  createdAt?: string;
  mustChangePassword?: boolean;
  accountLockedUntil?: string;
}

export interface TenantAdminRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

@Injectable({
  providedIn: 'root',
})
export class TenantService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/admin/tenants`;

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

  // Tenant Admin Management
  listTenantAdmins(tenantId: number): Observable<TenantAdminUser[]> {
    return this.http.get<TenantAdminUser[]>(`${this.apiUrl}/${tenantId}/admins`);
  }

  createTenantAdmin(tenantId: number, request: TenantAdminRequest): Observable<TenantAdminUser> {
    return this.http.post<TenantAdminUser>(`${this.apiUrl}/${tenantId}/admins`, request);
  }

  deactivateTenantAdmin(tenantId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${tenantId}/admins/${userId}`);
  }
}
