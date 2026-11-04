import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DocumentType } from '../models/document-type.model';
export { PageResponse } from '../models/page-response.model';
import { PageResponse } from '../models/page-response.model';

/**
 * Asks errorInterceptor not to show its own snackbar (#310). Each write below has one caller, a
 * reference-data page or form dialog that says what failed; reads take it only when asked.
 */
const SILENT = new HttpHeaders({ 'X-Silent-Error': 'true' });

/** The query these paged endpoints accept. Was `any`, which typed nothing (GH-158). */
export interface DocumentTypeFilters {
  page?: number;
  size?: number;
  name?: string;
  isRequired?: boolean;
}

export interface DocumentTypeResponse {
  id: number;
  typeName: string;
  description?: string;
  isRequired: boolean;
  isPlatformDefault?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentTypeRequest {
  name: string;
  description?: string;
  isRequired: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class DocumentTypeService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/document-type`;

  getAllDocumentTypes(): Observable<DocumentType[]> {
    return this.http.get<DocumentType[]>(this.apiUrl);
  }

  getRequiredDocumentTypes(tenantId?: number): Observable<DocumentType[]> {
    let params = new HttpParams();
    if (tenantId != null) {
      params = params.set('tenantId', tenantId.toString());
    }
    return this.http.get<DocumentType[]>(`${this.apiUrl}/required`, { params });
  }

  getAllDocumentTypesPaginated(
    filters: DocumentTypeFilters
  ): Observable<PageResponse<DocumentTypeResponse>> {
    const params = new HttpParams()
      .set('page', filters.page?.toString() || '0')
      .set('size', filters.size?.toString() || '20');

    return this.http.get<PageResponse<DocumentTypeResponse>>(this.apiUrl, { params });
  }
  searchDocumentTypes(
    filters: DocumentTypeFilters,
    silent = false
  ): Observable<PageResponse<DocumentTypeResponse>> {
    let params = new HttpParams()
      .set('page', filters.page?.toString() || '0')
      .set('size', filters.size?.toString() || '20');

    if (filters.name) {
      params = params.set('name', filters.name);
    }
    // The `!== ''` arm here was unreachable once the parameter had a type: isRequired is a
    // boolean, and comparing it to a string never matched (GH-158).
    if (filters.isRequired !== undefined && filters.isRequired !== null) {
      params = params.set('isRequired', filters.isRequired.toString());
    }

    return this.http.get<PageResponse<DocumentTypeResponse>>(`${this.apiUrl}/search`, {
      params,
      ...(silent ? { headers: SILENT } : {}),
    });
  }

  getDocumentTypeById(id: number): Observable<DocumentTypeResponse> {
    return this.http.get<DocumentTypeResponse>(`${this.apiUrl}/${id}`);
  }

  createDocumentType(request: DocumentTypeRequest): Observable<DocumentTypeResponse> {
    return this.http.post<DocumentTypeResponse>(this.apiUrl, request, { headers: SILENT });
  }

  updateDocumentType(id: number, request: DocumentTypeRequest): Observable<DocumentTypeResponse> {
    return this.http.put<DocumentTypeResponse>(`${this.apiUrl}/${id}`, request, {
      headers: SILENT,
    });
  }

  updateRequiredFlag(id: number, required: boolean): Observable<DocumentTypeResponse> {
    return this.http.patch<DocumentTypeResponse>(
      `${this.apiUrl}/${id}/required?required=${required}`,
      {},
      { headers: SILENT }
    );
  }

  deleteDocumentType(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers: SILENT });
  }
}
