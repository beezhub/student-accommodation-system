import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of, tap, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { YearOfStudy } from '../models/year-of-study.model';
export { PageResponse } from '../models/page-response.model';
import { PageResponse } from '../models/page-response.model';

/**
 * Asks errorInterceptor not to show its own snackbar (#310). Each write below has one caller, a
 * reference-data page or form dialog that says what failed; reads take it only when asked.
 */
const SILENT = new HttpHeaders({ 'X-Silent-Error': 'true' });

/** The query these paged endpoints accept. Was `any`, which typed nothing (GH-158). */
export interface YearOfStudyFilters {
  page?: number;
  size?: number;
  year?: string | number | null;
}

export interface YearOfStudyResponse {
  id: number;
  yearOfStudy: string;
}

export interface YearOfStudyRequest {
  yearOfStudy: string;
}

@Injectable({
  providedIn: 'root',
})
export class YearOfStudyService {
  private http = inject(HttpClient);

  private yearsOfStudyUrl = `${environment.apiUrl}/year-of-study`;
  private yearsCache: YearOfStudy[] | null = null;

  getYearsOfStudy(): Observable<YearOfStudy[]> {
    if (this.yearsCache) {
      return of(this.yearsCache);
    }

    // Fetch all years with a large page size to get all records
    const params = new HttpParams().set('page', '0').set('size', '100');

    return this.http.get<PageResponse<YearOfStudy>>(this.yearsOfStudyUrl, { params }).pipe(
      map((response) => response.content),
      tap((data) => (this.yearsCache = data))
    );
  }

  getAllYearsOfStudyPaginated(
    filters: YearOfStudyFilters
  ): Observable<PageResponse<YearOfStudyResponse>> {
    const params = new HttpParams()
      .set('page', filters.page?.toString() || '0')
      .set('size', filters.size?.toString() || '20');

    return this.http.get<PageResponse<YearOfStudyResponse>>(this.yearsOfStudyUrl, { params });
  }

  searchYearsOfStudy(
    filters: YearOfStudyFilters,
    silent = false
  ): Observable<PageResponse<YearOfStudyResponse>> {
    let params = new HttpParams()
      .set('page', filters.page?.toString() || '0')
      .set('size', filters.size?.toString() || '20');

    if (filters.year !== undefined && filters.year !== null && filters.year !== '') {
      params = params.set('year', filters.year.toString());
    }

    return this.http.get<PageResponse<YearOfStudyResponse>>(`${this.yearsOfStudyUrl}/search`, {
      params,
      ...(silent ? { headers: SILENT } : {}),
    });
  }

  getYearOfStudyById(id: number): Observable<YearOfStudyResponse> {
    return this.http.get<YearOfStudyResponse>(`${this.yearsOfStudyUrl}/${id}`);
  }

  createYearOfStudy(request: YearOfStudyRequest): Observable<YearOfStudyResponse> {
    return this.http.post<YearOfStudyResponse>(this.yearsOfStudyUrl, request, { headers: SILENT });
  }

  updateYearOfStudy(id: number, request: YearOfStudyRequest): Observable<YearOfStudyResponse> {
    return this.http.put<YearOfStudyResponse>(`${this.yearsOfStudyUrl}/${id}`, request, {
      headers: SILENT,
    });
  }

  deleteYearOfStudy(id: number): Observable<void> {
    return this.http.delete<void>(`${this.yearsOfStudyUrl}/${id}`, { headers: SILENT });
  }

  clearCache() {
    this.yearsCache = null;
  }
}
