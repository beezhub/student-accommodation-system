import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AcademicYear, AcademicYearRequest } from '../models/academic-year.model';

/**
 * Asks errorInterceptor not to show its own snackbar (#310). Each write below has one caller, a
 * reference-data page or form dialog that says what failed; reads take it only when asked.
 */
const SILENT = new HttpHeaders({ 'X-Silent-Error': 'true' });

@Injectable({
  providedIn: 'root',
})
export class AcademicYearService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/academic-years`;
  private currentYearCache: AcademicYear | null = null;

  /**
   * Get all academic years
   */
  getAcademicYears(silent = false): Observable<AcademicYear[]> {
    return this.http.get<AcademicYear[]>(this.apiUrl, silent ? { headers: SILENT } : {});
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
    return this.http
      .post<AcademicYear>(this.apiUrl, request, { headers: SILENT })
      .pipe(tap(() => this.clearCache()));
  }

  /**
   * Update an existing academic year (admin only)
   */
  updateAcademicYear(id: number, request: AcademicYearRequest): Observable<AcademicYear> {
    return this.http
      .put<AcademicYear>(`${this.apiUrl}/${id}`, request, { headers: SILENT })
      .pipe(tap(() => this.clearCache()));
  }

  /**
   * Delete an academic year (admin only)
   */
  deleteAcademicYear(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.apiUrl}/${id}`, { headers: SILENT })
      .pipe(tap(() => this.clearCache()));
  }

  /**
   * Set an academic year as the current one (admin only)
   */
  setCurrentAcademicYear(id: number): Observable<AcademicYear> {
    return this.http
      .post<AcademicYear>(`${this.apiUrl}/${id}/set-current`, {}, { headers: SILENT })
      .pipe(tap(() => this.clearCache()));
  }

  /**
   * Clear the current year cache
   */
  clearCache(): void {
    this.currentYearCache = null;
  }
}
