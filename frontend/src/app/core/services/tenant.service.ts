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

/**
 * An admin account with no company yet (#413). Made in user management, it waits locked until a
 * company's admin screen adds it. The list also holds BUILDING_STAFF and the deprecated ADMIN,
 * which the company screen does not offer.
 */
export interface UnassignedAdmin {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  userRole: string;
  tenantId: number;
  createdAt?: string;
  accountLockedUntil?: string;
}

@Injectable({
  providedIn: 'root',
})
export class TenantService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/admin/tenants`;
  private unassignedUrl = `${environment.apiUrl}/admin/unassigned-admins`;

  /**
   * Every tenant, active and inactive. This is what the tenant administration screen needs: it is
   * the only place a deactivated company can be switched back on, so it has to keep listing them.
   *
   * `silent` defaults to false because the user-management screen calls this with no error
   * handler of its own and relies entirely on the interceptor's generic message.
   */
  getAllTenants(silent = false): Observable<Tenant[]> {
    return this.http.get<Tenant[]>(
      this.apiUrl,
      silent ? { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) } : {}
    );
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
    return this.http.patch<Tenant>(
      `${this.apiUrl}/${id}/status`,
      { isActive },
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  // Tenant Admin Management
  listTenantAdmins(tenantId: number): Observable<TenantAdminUser[]> {
    return this.http.get<TenantAdminUser[]>(`${this.apiUrl}/${tenantId}/admins`, {
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
  }

  /**
   * Admin accounts waiting for a company. Users are created in user management only; a company's
   * admin screen picks one of these instead (#413). Silent: the picker shows its own message.
   */
  listUnassignedAdmins(): Observable<UnassignedAdmin[]> {
    return this.http.get<UnassignedAdmin[]>(this.unassignedUrl, {
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
  }

  /**
   * Gives a waiting account its company, which also unlocks it. Silent: the picker explains a
   * refusal inline, including a 409 when someone else assigned the account a moment earlier.
   */
  assignAdmin(userId: number, tenantId: number): Observable<UnassignedAdmin> {
    return this.http.post<UnassignedAdmin>(
      `${this.unassignedUrl}/${userId}/assign`,
      { tenantId },
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  deactivateTenantAdmin(tenantId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${tenantId}/admins/${userId}`, {
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
  }

  /** X-Silent-Error: the move dialog shows the error inline, so the snackbar would repeat it. */
  moveTenantAdmin(
    tenantId: number,
    userId: number,
    targetTenantId: number
  ): Observable<TenantAdminUser> {
    return this.http.post<TenantAdminUser>(
      `${this.apiUrl}/${tenantId}/admins/${userId}/move`,
      { targetTenantId },
      { headers: { 'X-Silent-Error': 'true' } }
    );
  }
}
