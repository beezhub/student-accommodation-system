import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from "../models/user.model";

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private apiUrl = `${environment.apiUrl}/documents`;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    const storedUser = localStorage.getItem('currentUser');
    if (storedUser) {
      this.currentUserSubject.next(JSON.parse(storedUser));
    }
   }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }
  uploadMultipleDocuments(files: File[], documentTypes: (string | number)[]): Observable<any> {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));

    // Build query params for documentTypes
    let params = new HttpParams();
    documentTypes.forEach(type => {
      params = params.append('documentTypes', type.toString());
    });

    console.log('Parameters:', params.toString());

    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
    
    return this.http.post(
      `${this.apiUrl}/upload-multiple`,
      formData,
      { headers, params }
    );
  }
}
