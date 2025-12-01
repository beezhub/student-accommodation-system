import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { DocumentType } from "../models/document-type.model";

@Injectable({
  providedIn: "root",
})
export class DocumentTypeService {
  private apiUrl = `${environment.apiUrl}/document-type`;

  constructor(private http: HttpClient) {}

  getAllDocumentTypes(): Observable<DocumentType[]> {
    return this.http.get<DocumentType[]>(this.apiUrl);
  }

  getRequiredDocumentTypes(): Observable<DocumentType[]> {
    return this.http.get<DocumentType[]>(`${this.apiUrl}/required`);
  }
}
