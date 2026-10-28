import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
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

  /**
   * Every tenant, active and inactive. This is what the tenant administration screen needs: it is
   * the only place a deactivated company can be switched back on, so it has to keep listing them.
   */
  getAllTenants(): Observable<Tenant[]> {
    return this.http.get<Tenant[]>(this.apiUrl);
  }

  /**
   * Only companies that may still be given new records. For dropdowns that assign a building or a
   * staff account to a company, where offering a deactivated one lets an admin go on attaching
   * records to a company that has been switched off (GH-267).
   *
   * <p>The backend refuses the same thing, because filtering here alone is cosmetic: the API still
   * accepts the id.
   *
   * <p>Silent because both callers (the building form's company selector and the create-user
   * dialog) show their own message on failure; the interceptor's generic one only stacked a
   * second toast on top of it.
   */
  getActiveTenants(): Observable<Tenant[]> {
    return this.http.get<Tenant[]>(this.apiUrl, {
      params: { activeOnly: true },
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
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
