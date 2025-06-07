import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Observable, BehaviorSubject } from "rxjs";
import { environment } from "../../../environments/environment";
import { DocumentType } from "../models/document-type.model";
import { User } from "../models/user.model";

@Injectable({
  providedIn: "root",
})
export class DocumentTypeService {
  private apiUrl = `${environment.apiUrl}/document-type`;
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

  getAllDocumentTypes(): Observable<DocumentType[]> {
    const token = localStorage.getItem("token");
        const headers = new HttpHeaders({
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        });
    return this.http.get<DocumentType[]>(this.apiUrl, { headers });
  }

  getRequiredDocumentTypes(): Observable<DocumentType[]> {
    const token = localStorage.getItem("token");
    const headers = new HttpHeaders({
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    });
    return this.http.get<DocumentType[]>(`${this.apiUrl}/required`, { headers });
  }
}
