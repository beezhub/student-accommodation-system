import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, map, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AcademicYear,
  AcademicYearPatternRequest,
  AcademicYearRequest,
  Intake,
  IntakeRequest,
} from '../models/academic-year.model';
import { PageResponse, contentOf } from '../models/page-response.model';

/**
 * Asks errorInterceptor not to show its own snackbar (#310). Each write below has one caller, a
 * reference-data page or form dialog that says what failed; reads take it only when asked.
 */
const SILENT = new HttpHeaders({ 'X-Silent-Error': 'true' });

/** Enough for every year a company keeps; the list is paginated on the server (GH-494). */
const LIST_PAGE_SIZE = 200;

@Injectable({
  providedIn: 'root',
})
export class AcademicYearService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/academic-years`;
  private intakesUrl = `${environment.apiUrl}/intakes`;
  private currentYearCache: AcademicYear | null = null;

  /** The caller's company's years with their intakes, newest first. */
  getAcademicYears(silent = false): Observable<AcademicYear[]> {
    const params = new HttpParams().set('size', LIST_PAGE_SIZE);
    return this.http
      .get<PageResponse<AcademicYear>>(
        this.apiUrl,
        silent ? { headers: SILENT, params } : { params }
      )
      .pipe(map((page) => contentOf(page)));
  }

  getAcademicYearById(id: number): Observable<AcademicYear> {
    return this.http.get<AcademicYear>(`${this.apiUrl}/${id}`);
  }

  /** The year whose dates contain today, or the next one between two years. */
  getCurrentAcademicYear(): Observable<AcademicYear> {
    if (this.currentYearCache) {
      return of(this.currentYearCache);
    }
    return this.http
      .get<AcademicYear>(`${this.apiUrl}/current`)
      .pipe(tap((year) => (this.currentYearCache = year)));
  }

  createAcademicYear(request: AcademicYearRequest): Observable<AcademicYear> {
    return this.write(this.http.post<AcademicYear>(this.apiUrl, request, { headers: SILENT }));
  }

  /** Creates a year with Annual, Semester or Trimester intakes and their windows filled in. */
  createFromPattern(request: AcademicYearPatternRequest): Observable<AcademicYear> {
    return this.write(
      this.http.post<AcademicYear>(`${this.apiUrl}/from-pattern`, request, { headers: SILENT })
    );
  }

  /** Copies a year and its intakes with every date a year later. */
  copyAcademicYear(id: number, name?: string): Observable<AcademicYear> {
    return this.write(
      this.http.post<AcademicYear>(`${this.apiUrl}/${id}/copy`, name ? { name } : {}, {
        headers: SILENT,
      })
    );
  }

  /** The response carries how many applications and allocations the year has. */
  updateAcademicYear(id: number, request: AcademicYearRequest): Observable<AcademicYear> {
    return this.write(
      this.http.put<AcademicYear>(`${this.apiUrl}/${id}`, request, { headers: SILENT })
    );
  }

  deleteAcademicYear(id: number): Observable<void> {
    return this.write(this.http.delete<void>(`${this.apiUrl}/${id}`, { headers: SILENT }));
  }

  createIntake(academicYearId: number, request: IntakeRequest): Observable<Intake> {
    return this.write(
      this.http.post<Intake>(`${this.apiUrl}/${academicYearId}/intakes`, request, {
        headers: SILENT,
      })
    );
  }

  /** The response carries how many applications and allocations the intake has. */
  updateIntake(id: number, request: IntakeRequest): Observable<Intake> {
    return this.write(
      this.http.put<Intake>(`${this.intakesUrl}/${id}`, request, { headers: SILENT })
    );
  }

  deleteIntake(id: number): Observable<void> {
    return this.write(this.http.delete<void>(`${this.intakesUrl}/${id}`, { headers: SILENT }));
  }

  /** Sets the intake's close date to today. */
  closeApplicationsNow(id: number): Observable<Intake> {
    return this.write(
      this.http.post<Intake>(`${this.intakesUrl}/${id}/close-applications`, {}, { headers: SILENT })
    );
  }

  clearCache(): void {
    this.currentYearCache = null;
  }

  private write<T>(request: Observable<T>): Observable<T> {
    return request.pipe(tap(() => this.clearCache()));
  }
}
