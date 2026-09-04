import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ActivityLog, ActivityLogFilters, ActivityLogResponse } from '../models/activity-log.model';

@Injectable({
  providedIn: 'root',
})
export class ActivityLogService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/admin/activity-logs`;

  /**
   * Get activity logs with filters
   */
  getActivityLogs(filters: ActivityLogFilters = {}): Observable<ActivityLogResponse> {
    let params = new HttpParams()
      .set('page', (filters.page || 0).toString())
      .set('size', (filters.size || 20).toString());

    if (filters.userId) params = params.set('userId', filters.userId.toString());
    if (filters.userRole) params = params.set('userRole', filters.userRole);
    if (filters.action) params = params.set('action', filters.action);
    if (filters.entityType) params = params.set('entityType', filters.entityType);
    if (filters.startDate) params = params.set('startDate', filters.startDate);
    if (filters.endDate) params = params.set('endDate', filters.endDate);

    return this.http.get<ActivityLogResponse>(this.apiUrl, { params });
  }

  /**
   * Get activity log by ID
   */
  getActivityLogById(id: number): Observable<ActivityLog> {
    return this.http.get<ActivityLog>(`${this.apiUrl}/${id}`);
  }

  /**
   * Export activity logs
   */
  exportActivityLogs(filters: ActivityLogFilters = {}): Observable<Blob> {
    let params = new HttpParams();

    if (filters.userId) params = params.set('userId', filters.userId.toString());
    if (filters.userRole) params = params.set('userRole', filters.userRole);
    if (filters.action) params = params.set('action', filters.action);
    if (filters.entityType) params = params.set('entityType', filters.entityType);
    if (filters.startDate) params = params.set('startDate', filters.startDate);
    if (filters.endDate) params = params.set('endDate', filters.endDate);

    return this.http.get(`${this.apiUrl}/export`, {
      params,
      responseType: 'blob',
    });
  }

  /**
   * Get action icon based on action type
   */
  getActionIcon(action: string): string {
    const iconMap: { [key: string]: string } = {
      CREATE: 'add_circle',
      UPDATE: 'edit',
      DELETE: 'delete',
      LOGIN: 'login',
      LOGOUT: 'logout',
      APPROVE: 'verified',
      REJECT: 'cancel',
      UPLOAD: 'upload',
      DOWNLOAD: 'download',
      VIEW: 'visibility',
      EXPORT: 'file_download',
    };
    return iconMap[action] || 'info';
  }

  /**
   * Get action color based on action type
   */
  getActionColor(action: string): string {
    const colorMap: { [key: string]: string } = {
      CREATE: 'success',
      UPDATE: 'primary',
      DELETE: 'warn',
      LOGIN: 'success',
      LOGOUT: 'neutral',
      APPROVE: 'success',
      REJECT: 'warn',
      UPLOAD: 'primary',
      DOWNLOAD: 'primary',
      VIEW: 'neutral',
      EXPORT: 'primary',
    };
    return colorMap[action] || 'neutral';
  }
}
