import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Institution {
  id: string;
  name: string;
  address: string;
  phoneNumber: string;
  emailAddress: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class InstitutionService {
  private apiUrl = `${environment.apiUrl}/institution`;

  constructor(private http: HttpClient) {}

  getInstitutions(): Observable<Institution[]> {
    const token = localStorage.getItem('token');
    console.log("token: " + token);
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
    console.log("Institution API URL:", this.apiUrl);
    console.log("Request Headers:", headers);
    return this.http.get<Institution[]>(this.apiUrl, { headers });
  }
}