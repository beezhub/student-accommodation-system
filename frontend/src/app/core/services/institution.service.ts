import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
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
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
    
    return this.http.get<Institution[]>(this.apiUrl, { headers }).pipe(
      tap(data => this.institutions = data)
    );
  }

  clearCache() {
    this.institutions = null;
  }

}