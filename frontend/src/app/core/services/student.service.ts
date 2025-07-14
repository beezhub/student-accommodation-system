import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { BehaviorSubject, Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { StudentRequest, StudentResponse } from "../models/student.model";
import { AuthService } from "./auth.service";

@Injectable({
  providedIn: "root",
})
export class StudentService {
  private apiUrl = `${environment.apiUrl}/student`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  createStudentProfile(
    studentData: StudentRequest
  ): Observable<StudentResponse> {
    const token = localStorage.getItem("token");
    const headers = new HttpHeaders({
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    });
    return this.http.post<StudentResponse>(`${this.apiUrl}`, studentData, {
      headers,
    });
  }

getStudentProfile(): Observable<StudentResponse> {
  const token = localStorage.getItem("token");
  const headers = new HttpHeaders({
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
  });

    const userId = this.authService.currentUserValue?.id
    if (!userId) {
      throw new Error("User does not exist or is not logged in.");
    }

    return this.http.get<StudentResponse>(`${this.apiUrl}/${userId}`, { headers });
  }
}
