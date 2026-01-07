import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
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

    return this.http.post(`${this.apiUrl}/upload-multiple`, formData, {
      params,
    });
  }

  uploadSingleDocument(file: File, documentTypeId: number): Observable<string> {
    const formData = new FormData();
    formData.append("file", file);

    let params = new HttpParams().set("documentType", documentTypeId.toString());

    return this.http.post(`${this.apiUrl}/upload-single`, formData, {
      params,
      responseType: 'text'
    });
  }

  getUploadedDocuments(): Observable<DocumentData[]> {
    const userId = this.authService.currentUserValue?.id;
    console.log('Fetching documents for user ID:', userId);

    return this.http.get<DocumentData[]>(`${this.apiUrl}/info/all/${userId}`);
  }

  getDocumentPreview(documentId: string | number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/${documentId}`, {
      responseType: 'blob'
    });
  }

  downloadDocument(documentId: string | number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/${documentId}`, {
      responseType: 'blob'
    });
  }
}
