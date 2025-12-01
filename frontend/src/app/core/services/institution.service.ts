import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Institution } from '../models/institution.model';

@Injectable({
  providedIn: 'root'
})
export class InstitutionService {
  private apiUrl = `${environment.apiUrl}/institution`;
 private institutions: Institution[] | null = null;

  constructor(private http: HttpClient) {}

  getInstitutions(): Observable<Institution[]> {
    if (this.institutions) {
      return of(this.institutions);
    }

    return this.http.get<Institution[]>(this.apiUrl).pipe(
      tap(data => this.institutions = data)
    );
  }

  clearCache() {
    this.institutions = null;
  }

}