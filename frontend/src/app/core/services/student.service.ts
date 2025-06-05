import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { BehaviorSubject, Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { User } from "../../model/user.model";
import { StudentRequest, StudentResponse } from "../models/student.model";

@Injectable({
  providedIn: "root",
})
export class StudentService {
  private apiUrl = `${environment.apiUrl}/student`;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    const storedUser = localStorage.getItem("currentUser");
    if (storedUser) {
      this.currentUserSubject.next(JSON.parse(storedUser));
    }
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  createStudentProfile(studentData: StudentRequest): Observable<StudentResponse> {
    const token = localStorage.getItem("token");
    console.log(token);
    const headers = new HttpHeaders({
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    });
    return this.http
      .post<StudentResponse>(`${this.apiUrl}`, studentData, { headers });
  }
}
