import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AllocationRule } from '../../model/allocation-rule.model';

@Injectable({
  providedIn: 'root',
})
export class AllocationRuleService {
  private apiUrl = `${environment.apiUrl}/allocation-rules`;

  constructor(private http: HttpClient) {}

  getRulesForBuilding(buildingId: number): Observable<AllocationRule[]> {
    return this.http.get<AllocationRule[]>(`${this.apiUrl}/building/${buildingId}`);
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
