import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AllocationRule } from '../models/allocation.models';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root',
})
export class AllocationRuleService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/admin/allocation-rules`;

  getRulesForBuilding(buildingId: number | null): Observable<AllocationRule[]> {
    const params = buildingId !== null ? `?buildingId=${buildingId}&size=100` : '?size=100';
    return this.http
      .get<PageResponse<AllocationRule>>(`${this.apiUrl}${params}`)
      .pipe(map((response) => response.content));
  }

  getRule(ruleId: number): Observable<AllocationRule> {
    return this.http.get<AllocationRule>(`${this.apiUrl}/${ruleId}`);
  }

  createRule(rule: Partial<AllocationRule>): Observable<AllocationRule> {
    return this.http.post<AllocationRule>(this.apiUrl, rule);
  }

  updateRule(ruleId: number, rule: Partial<AllocationRule>): Observable<AllocationRule> {
    return this.http.put<AllocationRule>(`${this.apiUrl}/${ruleId}`, rule);
  }

  deleteRule(ruleId: number): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/${ruleId}`);
  }

  activateRule(ruleId: number): Observable<AllocationRule> {
    return this.http.patch<AllocationRule>(`${this.apiUrl}/${ruleId}/activate`, {});
  }
}
