import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { StudentRequest, StudentResponse } from '../models/student.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class StudentService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);

  private apiUrl = `${environment.apiUrl}/student`;

  createStudentProfile(studentData: StudentRequest): Observable<StudentResponse> {
    return this.http.post<StudentResponse>(`${this.apiUrl}`, studentData);
  }

  getStudentProfile(): Observable<StudentResponse> {
    return this.http.get<StudentResponse>(`${this.apiUrl}/me`);
  }

  updateStudentProfile(studentData: StudentRequest): Observable<StudentResponse> {
    const userId = this.authService.currentUserValue?.id;
    if (!userId) {
      throw new Error('User does not exist or is not logged in.');
    }

    return this.http.put<StudentResponse>(`${this.apiUrl}/${userId}`, studentData);
  }
}
