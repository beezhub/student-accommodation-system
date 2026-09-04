import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DocumentComment } from '../models/allocation.models';

@Injectable({
  providedIn: 'root',
})
export class DocumentCommentService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/documents`;

  addComment(documentId: number, text: string): Observable<DocumentComment> {
    return this.http.post<DocumentComment>(`${this.apiUrl}/${documentId}/comments`, {
      commentText: text,
    });
  }

  getComments(documentId: number): Observable<DocumentComment[]> {
    return this.http.get<DocumentComment[]>(`${this.apiUrl}/${documentId}/comments`);
  }

  deleteComment(commentId: number): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/comments/${commentId}`);
  }
}
