import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AccommodationType {
  id: number;
  name: string;
  description?: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AccommodationTypeRequest {
  name: string;
  description?: string;
  isActive?: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root',
})
export class AccommodationTypeService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/accommodation-types`;

  /**
   * Get all active accommodation types (for student forms)
   */
  getActiveTypes(): Observable<AccommodationType[]> {
    return this.http.get<AccommodationType[]>(`${this.apiUrl}/active`);
  }

  /**
   * Get all accommodation types with pagination (admin only)
   */
  getAll(page = 0, size = 20, sort = 'name'): Observable<PageResponse<AccommodationType>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.get<PageResponse<AccommodationType>>(this.apiUrl, { params });
  }

  /**
   * Get accommodation type by ID (admin only)
   */
  getById(id: number): Observable<AccommodationType> {
    return this.http.get<AccommodationType>(`${this.apiUrl}/${id}`);
  }

  /**
   * Create a new accommodation type (admin only)
   */
  create(request: AccommodationTypeRequest): Observable<AccommodationType> {
    return this.http.post<AccommodationType>(this.apiUrl, request);
  }

  /**
   * Update an existing accommodation type (admin only)
   */
  update(id: number, request: AccommodationTypeRequest): Observable<AccommodationType> {
    return this.http.put<AccommodationType>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Delete an accommodation type (admin only)
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
