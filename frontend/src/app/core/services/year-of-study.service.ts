import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, tap, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { YearOfStudy } from '../models/year-of-study.model';
export { PageResponse } from '../models/page-response.model';
import { PageResponse } from '../models/page-response.model';

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

  getAllYearsOfStudyPaginated(filters: any): Observable<PageResponse<YearOfStudyResponse>> {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const params = new HttpParams()
      .set('page', filters.page?.toString() || '0')
      .set('size', filters.size?.toString() || '20');

    return this.http.get<PageResponse<YearOfStudyResponse>>(this.yearsOfStudyUrl, { params });
  }

  searchYearsOfStudy(filters: any): Observable<PageResponse<YearOfStudyResponse>> {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let params = new HttpParams()
      .set('page', filters.page?.toString() || '0')
      .set('size', filters.size?.toString() || '20');

    if (filters.year !== undefined && filters.year !== null && filters.year !== '') {
      params = params.set('year', filters.year.toString());
    }

    return this.http.get<PageResponse<YearOfStudyResponse>>(`${this.yearsOfStudyUrl}/search`, {
      params,
    });
  }

  getYearOfStudyById(id: number): Observable<YearOfStudyResponse> {
    return this.http.get<YearOfStudyResponse>(`${this.yearsOfStudyUrl}/${id}`);
  }

  createYearOfStudy(request: YearOfStudyRequest): Observable<YearOfStudyResponse> {
    return this.http.post<YearOfStudyResponse>(this.yearsOfStudyUrl, request);
  }

  updateYearOfStudy(id: number, request: YearOfStudyRequest): Observable<YearOfStudyResponse> {
    return this.http.put<YearOfStudyResponse>(`${this.yearsOfStudyUrl}/${id}`, request);
  }

  deleteYearOfStudy(id: number): Observable<void> {
    return this.http.delete<void>(`${this.yearsOfStudyUrl}/${id}`);
  }

  clearCache() {
    this.yearsCache = null;
  }
}
