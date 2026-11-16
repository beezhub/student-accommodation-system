import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface WaitlistPosition {
  position: number;
  totalInQueue: number;
  /** Null when there isn't enough recent allocation history to estimate from. */
  estimatedWaitDays: number | null;
}

@Injectable({ providedIn: 'root' })
export class WaitlistService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/waitlist`;

  /**
   * Fetches the authenticated student's waitlist position. Resolves to
   * `null` (rather than erroring) when the student has no active waiting-list
   * entry (backend returns 404), so callers can simply hide the waitlist card.
   * Sends `X-Silent-Error` because the 404 is an expected answer handled here,
   * so `errorInterceptor` must not show its snackbar for it.
   */
  getMyPosition(): Observable<WaitlistPosition | null> {
    return this.http
      .get<WaitlistPosition>(`${this.apiUrl}/position`, {
        headers: { 'X-Silent-Error': 'true' },
      })
      .pipe(catchError(() => of(null)));
  }
}
