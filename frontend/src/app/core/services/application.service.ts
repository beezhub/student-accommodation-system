import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { Application } from "../models/application.model";

@Injectable({
  providedIn: "root",
})
export class ApplicationService {
  private apiUrl = `${environment.apiUrl}/applications`;

  constructor(private http: HttpClient) {}

  createApplication(applicationData: any): Observable<Application> {
    return this.http.post<Application>(
      `${this.apiUrl}/submit`,
      applicationData
    );
  }

  getApplicationByStatus(status: string): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/${status}`);
  }

  getApplicationById(applicationId: number): Observable<Application> {
    return this.http.get<Application>(`${this.apiUrl}/${applicationId}`);
  }

  getMyApplications(): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/my-applications`);
  }

}
