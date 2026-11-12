import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, of, shareReplay } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ContactInfo } from '../models/contact-info.model';

/** The pages that call this show their own fallback, so no error snackbar. */
const SILENT = new HttpHeaders({ 'X-Silent-Error': 'true' });

@Injectable({
  providedIn: 'root',
})
export class ContactInfoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/contact`;
  private cached$?: Observable<ContactInfo | null>;

  /**
   * The details only change on a redeploy, so the first answer is kept for the session.
   * Emits null when the request fails, and forgets it so the next caller tries again.
   */
  getContactInfo(): Observable<ContactInfo | null> {
    this.cached$ ??= this.http.get<ContactInfo>(this.apiUrl, { headers: SILENT }).pipe(
      catchError(() => {
        this.cached$ = undefined;
        return of(null);
      }),
      shareReplay(1)
    );
    return this.cached$;
  }
}
