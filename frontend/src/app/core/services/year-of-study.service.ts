import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface YearOfStudy {
  id: string;
  yearOfStudy: string;
}

@Injectable({
  providedIn: 'root'
})

export class YearOfStudyService {
  private yearsOfStudyUrl = `${environment.apiUrl}/year-of-study`;

  constructor(private http: HttpClient) { }

  
  getYearsOfStudy(): Observable<YearOfStudy[]> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
    return this.http.get<YearOfStudy[]>(this.yearsOfStudyUrl, { headers });
  }

}
