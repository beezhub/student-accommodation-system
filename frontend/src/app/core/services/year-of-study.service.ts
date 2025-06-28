import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { YearOfStudy } from '../models/year-of-study.model';

@Injectable({
  providedIn: 'root'
})

export class YearOfStudyService {
  private yearsOfStudyUrl = `${environment.apiUrl}/year-of-study`;
  private yearsCache: YearOfStudy[] | null = null;

  constructor(private http: HttpClient) { }

  getYearsOfStudy(): Observable<YearOfStudy[]> {
    if (this.yearsCache) {
      return of(this.yearsCache);
    }
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
    return this.http.get<YearOfStudy[]>(this.yearsOfStudyUrl, { headers }).pipe(
      tap(data => this.yearsCache = data)
    );
  }

  clearCache() {
    this.yearsCache = null;
  }

}
