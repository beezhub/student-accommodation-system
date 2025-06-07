import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { Application } from "../models/application.model";
import { AuthService } from "./auth.service";

@Injectable({
  providedIn: "root",
})
export class ApplicationService {
  private apiUrl = `${environment.apiUrl}/applications`;

  constructor(private http: HttpClient, private authService: AuthService) {}
  createApplication(studentId: number): Observable<Application> {
    let params = new HttpParams();
    const token = localStorage.getItem("token");
    const headers = {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    };
    params = params.append("studentId", studentId);
    return this.http.post<Application>(
      `${this.apiUrl}/submit`,{
        headers,
        params
      }
    );
  }

  getApplicationByStatus(status: string): Observable<Application[]> {
    const token = localStorage.getItem("token");
    const headers = {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    };
    return this.http.get<Application[]>(`${this.apiUrl}/${status}`, { headers });
  }
}
