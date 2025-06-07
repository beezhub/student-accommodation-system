import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { BehaviorSubject, map, Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { User } from "../models/user.model";
import { SignupData } from "../models/signup-data.model";
import { InstitutionService } from "./institution.service";
import { YearOfStudyService } from "./year-of-study.service";

@Injectable({
  providedIn: "root",
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private institutionService: InstitutionService,
    private yearOfStudyService: YearOfStudyService
  ) {
    window.addEventListener("storage", (event) => {
      if (event.key === "currentUser") {
        const updatedUser = event.newValue ? JSON.parse(event.newValue) : null;
        this.currentUserSubject.next(updatedUser);
      }
    });
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  signup(data: SignupData): Observable<User> {
    return this.http
      .post<{ token: string; user: User }>(`${this.apiUrl}/signup`, data)
      .pipe(
        map((response) => {
          localStorage.setItem("token", response.token);
          localStorage.setItem("currentUser", JSON.stringify(response.user));
          this.currentUserSubject.next(response.user);
          return response.user;
        })
      );
  }

  uploadDocuments(files: FormData): Observable<any> {
    return this.http.post(`${this.apiUrl}/documents`, files);
  }

  submitApplication(applicationData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/submit-application`, applicationData);
  }

  logout(): void {
    localStorage.removeItem("currentUser");
    localStorage.removeItem("token");
    this.currentUserSubject.next(null);
    this.institutionService.clearCache();
    this.yearOfStudyService.clearCache();
  }
  // Login a user
  login(data: any): Observable<User> {
    return this.http
      .post<{ token: string; user: User }>(`${this.apiUrl}/login`, data)
      .pipe(
        map((response) => {
          localStorage.setItem("token", response.token);
          localStorage.setItem("currentUser", JSON.stringify(response.user));
          this.currentUserSubject.next(response.user);
          return response.user;
        })
      );
  }

  isTokenExpired(): boolean {
    const token = localStorage.getItem("token");
    if (!token) return true;
    try {
      const payload = JSON.parse(atob(token.split(".")[1]));
      const expiry = payload.exp;
      return Math.floor(Date.now() / 1000) > expiry;
    } catch (e) {
      return true;
    }
  }
}
