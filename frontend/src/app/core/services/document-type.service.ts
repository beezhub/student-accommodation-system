import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Observable, BehaviorSubject } from "rxjs";
import { environment } from "../../../environments/environment";
import { DocumentType } from "../models/document-type.model";
import { User } from "../models/user.model";
import { AuthService } from "./auth.service";

@Injectable({
  providedIn: "root",
})
export class DocumentTypeService {
  private apiUrl = `${environment.apiUrl}/document-type`;


  constructor(private http: HttpClient, private authService: AuthService) {}
    


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
