import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { DocumentType } from "../models/document-type.model";

export interface DocumentTypeResponse {
  id: number;
  typeName: string;
  description?: string;
  isRequired: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentTypeRequest {
  name: string;
  description?: string;
  isRequired: boolean;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

@Injectable({
  providedIn: "root",
})
export class DocumentTypeService {
  private apiUrl = `${environment.apiUrl}/document-type`;

  constructor(private http: HttpClient) {}

  getAllDocumentTypes(): Observable<DocumentType[]> {
    return this.http.get<DocumentType[]>(this.apiUrl);
  }

  getRequiredDocumentTypes(): Observable<DocumentType[]> {
    return this.http.get<DocumentType[]>(`${this.apiUrl}/required`);
  }

  getAllDocumentTypesPaginated(filters: any): Observable<PageResponse<DocumentTypeResponse>> {
    let params = new HttpParams()
      .set('page', filters.page?.toString() || '0')
      .set('size', filters.size?.toString() || '20');

    return this.http.get<PageResponse<DocumentTypeResponse>>(this.apiUrl, { params });
  }

  searchDocumentTypes(filters: any): Observable<PageResponse<DocumentTypeResponse>> {
    let params = new HttpParams()
      .set('page', filters.page?.toString() || '0')
      .set('size', filters.size?.toString() || '20');

    if (filters.name) {
      params = params.set('name', filters.name);
    }
    if (filters.isRequired !== undefined && filters.isRequired !== null && filters.isRequired !== '') {
      params = params.set('isRequired', filters.isRequired.toString());
    }

    return this.http.get<PageResponse<DocumentTypeResponse>>(`${this.apiUrl}/search`, { params });
  }

  getDocumentTypeById(id: number): Observable<DocumentTypeResponse> {
    return this.http.get<DocumentTypeResponse>(`${this.apiUrl}/${id}`);
  }

  createDocumentType(request: DocumentTypeRequest): Observable<DocumentTypeResponse> {
    return this.http.post<DocumentTypeResponse>(this.apiUrl, request);
  }

  updateDocumentType(id: number, request: DocumentTypeRequest): Observable<DocumentTypeResponse> {
    return this.http.put<DocumentTypeResponse>(`${this.apiUrl}/${id}`, request);
  }

  updateRequiredFlag(id: number, required: boolean): Observable<DocumentTypeResponse> {
    return this.http.patch<DocumentTypeResponse>(
      `${this.apiUrl}/${id}/required?required=${required}`,
      {}
    );
  }

  deleteDocumentType(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
