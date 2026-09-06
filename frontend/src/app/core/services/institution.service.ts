import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Institution } from '../models/institution.model';
export { PageResponse } from '../models/page-response.model';
import { PageResponse } from '../models/page-response.model';

/** The query these paged endpoints accept. Was `any`, which typed nothing (GH-158). */
export interface InstitutionFilters {
  page?: number;
  size?: number;
  name?: string;
  address?: string;
  contactEmail?: string;
}

export interface InstitutionResponse {
  id: number;
  name: string;
  address?: string;
  phoneNumber?: string;
  emailAddress?: string;
  createdAt: string;
  updatedAt: string;
}

export interface InstitutionRequest {
  name: string;
  address: string;
  phoneNumber: string;
  emailAddress: string;
}

@Injectable({
  providedIn: 'root',
})
export class InstitutionService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/institution`;
  private institutions: Institution[] | null = null;

  getInstitutions(): Observable<Institution[]> {
    if (this.institutions) {
      return of(this.institutions);
    }

    return this.http
      .get<Institution[]>(`${this.apiUrl}/all`)
      .pipe(tap((data) => (this.institutions = data)));
  }

  getAllInstitutions(filters: InstitutionFilters): Observable<PageResponse<InstitutionResponse>> {
    const params = new HttpParams()
      .set('page', filters.page?.toString() || '0')
      .set('size', filters.size?.toString() || '20');

    return this.http.get<PageResponse<InstitutionResponse>>(this.apiUrl, { params });
  }

  searchInstitutions(filters: InstitutionFilters): Observable<PageResponse<InstitutionResponse>> {
    let params = new HttpParams()
      .set('page', filters.page?.toString() || '0')
      .set('size', filters.size?.toString() || '20');

    if (filters.name) {
      params = params.set('name', filters.name);
    }
    if (filters.address) {
      params = params.set('address', filters.address);
    }
    if (filters.contactEmail) {
      params = params.set('contactEmail', filters.contactEmail);
    }

    return this.http.get<PageResponse<InstitutionResponse>>(`${this.apiUrl}/search`, { params });
  }

  getInstitutionById(id: number): Observable<InstitutionResponse> {
    return this.http.get<InstitutionResponse>(`${this.apiUrl}/${id}`);
  }

  createInstitution(request: InstitutionRequest): Observable<InstitutionResponse> {
    this.clearCache();
    return this.http.post<InstitutionResponse>(this.apiUrl, request);
  }

  updateInstitution(id: number, request: InstitutionRequest): Observable<InstitutionResponse> {
    this.clearCache();
    return this.http.put<InstitutionResponse>(`${this.apiUrl}/${id}`, request);
  }

  deleteInstitution(id: number): Observable<void> {
    this.clearCache();
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  clearCache() {
    this.institutions = null;
  }
}
