import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page-response.model';
import { StudentRequest, StudentResponse } from '../models/student.model';
import { AuthService } from './auth.service';

/** The query the student list endpoints accept. Was `any`, which typed nothing (GH-158). */
export interface StudentFilters {
  page?: number;
  size?: number;
  studentNumber?: string;
  name?: string;
  institutionId?: number;
}

/** Drops undefined entries, which HttpParams would otherwise send as the string "undefined". */
function toHttpParams(filters?: StudentFilters): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(filters ?? {})) {
    if (value !== undefined && value !== null && value !== '') {
      params = params.set(key, String(value));
    }
  }
  return params;
}

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

  getAllStudents(params?: StudentFilters): Observable<PageResponse<StudentResponse>> {
    return this.http.get<PageResponse<StudentResponse>>(`${this.apiUrl}`, {
      params: toHttpParams(params),
    });
  }

  searchStudents(params?: StudentFilters): Observable<PageResponse<StudentResponse>> {
    return this.http.get<PageResponse<StudentResponse>>(`${this.apiUrl}/search`, {
      params: toHttpParams(params),
    });
  }

  deleteStudent(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
