import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

/** A suggestion as the admin types. No coordinates yet: those come from `resolve`. */
export interface AddressSuggestion {
  placeId: string;
  displayName: string;
}

export interface ResolvedPlace {
  placeId: string;
  latitude: number;
  longitude: number;
}

/**
 * Address lookup for the building form (GH-152).
 *
 * <p>Was `NominatimService`, which called nominatim.openstreetmap.org from the browser. Two
 * problems with that, beyond the provider: every visitor's browser talked to a third party on the
 * page's behalf, and `BuildingController` handed out a `dummy-api-key-for-development` for a
 * paid API that nothing called. Both are gone — the lookup is a backend endpoint, so the key
 * stays on the server and the request is authorized.
 *
 * <p>The backend uses Google Places Autocomplete, so lookup is two steps: `search` while typing,
 * then `resolve` for the one suggestion picked. Pass the same session token to both, so Google
 * bills the whole pick as one session instead of once per keystroke.
 */
@Injectable({
  providedIn: 'root',
})
export class GeocodingService {
  private http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiUrl}/geocoding`;
  private readonly mapsConfigUrl = `${environment.apiUrl}/building/maps-config`;

  /** Shortest query worth a request. Mirrors the backend, which will not spend one either. */
  private readonly MIN_QUERY_LENGTH = 3;

  /**
   * A new token for one pick. Undefined where `crypto.randomUUID` is missing (plain-http pages
   * other than localhost); the lookup still works, it is just billed per request.
   */
  newSessionToken(): string | undefined {
    return globalThis.crypto?.randomUUID?.();
  }

  search(query: string, sessionToken?: string): Observable<AddressSuggestion[]> {
    if (!query || query.trim().length < this.MIN_QUERY_LENGTH) {
      return of([]);
    }

    let params = new HttpParams().set('q', query.trim());
    if (sessionToken) {
      params = params.set('session', sessionToken);
    }

    // X-Silent-Error: a 503 here means geocoding is not configured or the provider is down, and
    // the form already degrades to plain typing. A snackbar on every keystroke would be worse
    // than the missing suggestions.
    return this.http
      .get<AddressSuggestion[]>(`${this.apiUrl}/search`, {
        params,
        headers: { 'X-Silent-Error': 'true' },
      })
      .pipe(catchError(() => of([])));
  }

  /** Coordinates of a picked suggestion, or null when they could not be fetched. */
  resolve(placeId: string, sessionToken?: string): Observable<ResolvedPlace | null> {
    const params = sessionToken ? new HttpParams().set('session', sessionToken) : undefined;

    return this.http
      .get<ResolvedPlace>(`${this.apiUrl}/places/${encodeURIComponent(placeId)}`, {
        params,
        headers: { 'X-Silent-Error': 'true' },
      })
      .pipe(catchError(() => of(null)));
  }

  /**
   * Whether address lookup is available at all, so the form can say so rather than leaving an
   * administrator typing into a box that never suggests anything.
   */
  isEnabled(): Observable<boolean> {
    return this.http
      .get<{ enabled: boolean }>(this.mapsConfigUrl, {
        headers: { 'X-Silent-Error': 'true' },
      })
      .pipe(
        map((config) => config.enabled === true),
        catchError(() => of(false))
      );
  }
}
