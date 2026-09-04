import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

export interface NominatimResult {
  place_id: number;
  display_name: string;
  lat: string;
  lon: string;
  address?: {
    road?: string;
    city?: string;
    town?: string;
    village?: string;
    state?: string;
    country?: string;
    postcode?: string;
  };
}

export interface AddressSuggestion {
  placeId: string;
  displayName: string;
  latitude: number;
  longitude: number;
}

@Injectable({
  providedIn: 'root',
})
export class NominatimService {
  private http = inject(HttpClient);

  private readonly BASE_URL = 'https://nominatim.openstreetmap.org/search';

  search(query: string): Observable<AddressSuggestion[]> {
    if (!query || query.trim().length < 3) {
      return of([]);
    }

    const params = new HttpParams()
      .set('q', query.trim())
      .set('format', 'json')
      .set('addressdetails', '1')
      .set('limit', '5')
      .set('dedupe', '1');

    return this.http
      .get<NominatimResult[]>(this.BASE_URL, {
        params,
        headers: {
          'Accept-Language': 'en',
          'User-Agent': 'StudentAccommodationSystem/1.0',
        },
      })
      .pipe(
        map((results) =>
          results.map((r) => ({
            placeId: String(r.place_id),
            displayName: r.display_name,
            latitude: parseFloat(r.lat),
            longitude: parseFloat(r.lon),
          }))
        ),
        catchError(() => of([]))
      );
  }
}
