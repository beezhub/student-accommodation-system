import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AcademicYear, AcademicYearRequest } from '../models/academic-year.model';

@Injectable({
  providedIn: 'root',
})
export class AcademicYearService {
  private apiUrl = `${environment.apiUrl}/academic-years`;
  private currentYearCache: AcademicYear | null = null;

  constructor(private http: HttpClient) {}

  /**
   * Get all academic years
   */
  getAcademicYears(): Observable<AcademicYear[]> {
    return this.http.get<AcademicYear[]>(this.apiUrl);
  }

  /**
   * Get academic year by ID
   */
  getAcademicYearById(id: number): Observable<AcademicYear> {
    return this.http.get<AcademicYear>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get the current academic year
   */
  getCurrentAcademicYear(): Observable<AcademicYear> {
    if (this.currentYearCache) {
      return of(this.currentYearCache);
    }
    return this.http
      .get<AcademicYear>(`${this.apiUrl}/current`)
      .pipe(tap((year) => (this.currentYearCache = year)));
  }

  /**
   * Create a new academic year (admin only)
   */
  createAcademicYear(request: AcademicYearRequest): Observable<AcademicYear> {
    return this.http.post<AcademicYear>(this.apiUrl, request).pipe(tap(() => this.clearCache()));
  }

  /**
   * Update an existing academic year (admin only)
   */
  updateAcademicYear(id: number, request: AcademicYearRequest): Observable<AcademicYear> {
    return this.http
      .put<AcademicYear>(`${this.apiUrl}/${id}`, request)
      .pipe(tap(() => this.clearCache()));
  }

  /**
   * Delete an academic year (admin only)
   */
  deleteAcademicYear(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(tap(() => this.clearCache()));
  }

  /**
   * Set an academic year as the current one (admin only)
   */
  setCurrentAcademicYear(id: number): Observable<AcademicYear> {
    return this.http
      .post<AcademicYear>(`${this.apiUrl}/${id}/set-current`, {})
      .pipe(tap(() => this.clearCache()));
  }

  /**
   * Clear the current year cache
   */
  clearCache(): void {
    this.currentYearCache = null;
  }
}
