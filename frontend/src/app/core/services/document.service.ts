import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders, HttpParams } from "@angular/common/http";
import { BehaviorSubject, Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { User } from "../models/user.model";
import { DocumentData } from "../models/document.model";
import { AuthService } from "./auth.service";

@Injectable({
  providedIn: "root",
})
export class DocumentService {
  private apiUrl = `${environment.apiUrl}/documents`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  uploadMultipleDocuments(
    files: File[],
    documentTypes: (string | number)[]
  ): Observable<any> {
    const formData = new FormData();
    files.forEach((file) => formData.append("files", file));

    let params = new HttpParams();
    documentTypes.forEach((type) => {
      params = params.append("documentTypes", type.toString());
    });

    console.log("Parameters:", params.toString());

    const token = localStorage.getItem("token");
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
    });

    return this.http.post(`${this.apiUrl}/upload-multiple`, formData, {
      headers,
      params,
    });
  }

  getUploadedDocuments(): Observable<DocumentData[]> {
    const token = localStorage.getItem("token");
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
    });

    const userId = this.authService.currentUserValue?.id;
    console.log('Fetching documents for user ID:', userId);

    return this.http.get<DocumentData[]>(`${this.apiUrl}/info/all/${userId}`, {
      headers,
    });
  }
}
