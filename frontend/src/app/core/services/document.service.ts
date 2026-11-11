import { Injectable, inject } from '@angular/core';
import {
  HttpClient,
  HttpEvent,
  HttpEventType,
  HttpHeaders,
  HttpParams,
} from '@angular/common/http';
import { Observable, map, filter } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DocumentData, DocumentExpiry } from '../models/document.model';
import { AuthService } from './auth.service';

export type UploadProgress =
  | { status: 'progress'; progress: number }
  | { status: 'complete'; progress: number; message: string };

export interface FileValidationResult {
  valid: boolean;
  errors: string[];
}

@Injectable({
  providedIn: 'root',
})
export class DocumentService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);

  private apiUrl = `${environment.apiUrl}/documents`;

  // File validation constants
  private readonly MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
  private readonly ALLOWED_TYPES = ['application/pdf', 'image/jpeg', 'image/png'];
  private readonly ALLOWED_EXTENSIONS = ['.pdf', '.jpg', '.jpeg', '.png'];

  /**
   * Validates a file before upload
   */
  validateFile(file: File): FileValidationResult {
    const errors: string[] = [];

    // Check file size
    if (file.size > this.MAX_FILE_SIZE) {
      const maxSizeMB = this.MAX_FILE_SIZE / (1024 * 1024);
      const fileSizeMB = (file.size / (1024 * 1024)).toFixed(2);
      errors.push(`File size (${fileSizeMB}MB) exceeds maximum allowed size of ${maxSizeMB}MB`);
    }

    // Check file type
    if (!this.ALLOWED_TYPES.includes(file.type)) {
      errors.push(
        `File type "${file.type || 'unknown'}" is not allowed. Accepted types: PDF, JPG, PNG`
      );
    }

    // Check file extension as backup
    const extension = '.' + file.name.split('.').pop()?.toLowerCase();
    if (!this.ALLOWED_EXTENSIONS.includes(extension)) {
      errors.push(
        `File extension "${extension}" is not allowed. Accepted extensions: ${this.ALLOWED_EXTENSIONS.join(
          ', '
        )}`
      );
    }

    // Check if file is empty
    if (file.size === 0) {
      errors.push('File appears to be empty');
    }

    return {
      valid: errors.length === 0,
      errors,
    };
  }

  /**
   * Formats file size for display
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  /**
   * Upload single document with progress tracking
   */
  uploadSingleDocumentWithProgress(file: File, documentTypeId: number): Observable<UploadProgress> {
    const formData = new FormData();
    formData.append('file', file);

    const params = new HttpParams().set('documentType', documentTypeId.toString());

    return this.http
      .post(`${this.apiUrl}/upload-single`, formData, {
        params,
        reportProgress: true,
        observe: 'events',
        responseType: 'text',
      })
      .pipe(
        map((event: HttpEvent<string>) => {
          switch (event.type) {
            case HttpEventType.UploadProgress:
              const progress = event.total ? Math.round((100 * event.loaded) / event.total) : 0;
              return { status: 'progress' as const, progress };
            case HttpEventType.Response:
              return { status: 'complete' as const, progress: 100, message: 'Upload complete' };
            default:
              return { status: 'progress' as const, progress: 0 };
          }
        }),
        filter(
          (result): result is UploadProgress =>
            result.status === 'progress' || result.status === 'complete'
        )
      );
  }

  /**
   * Upload multiple documents with progress tracking
   */
  uploadMultipleDocumentsWithProgress(
    files: File[],
    documentTypes: (string | number)[]
  ): Observable<UploadProgress> {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));

    let params = new HttpParams();
    documentTypes.forEach((type) => {
      params = params.append('documentTypes', type.toString());
    });

    return this.http
      .post(`${this.apiUrl}/upload-multiple`, formData, {
        params,
        reportProgress: true,
        observe: 'events',
      })
      .pipe(
        map((event: HttpEvent<unknown>) => {
          switch (event.type) {
            case HttpEventType.UploadProgress:
              const progress = event.total ? Math.round((100 * event.loaded) / event.total) : 0;
              return { status: 'progress' as const, progress };
            case HttpEventType.Response:
              return {
                status: 'complete' as const,
                progress: 100,
                message: 'All documents uploaded',
              };
            default:
              return { status: 'progress' as const, progress: 0 };
          }
        }),
        filter(
          (result): result is UploadProgress =>
            result.status === 'progress' || result.status === 'complete'
        )
      );
  }

  uploadMultipleDocuments(
    files: File[],
    documentTypes: (string | number)[]
  ): Observable<DocumentData[]> {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));

    let params = new HttpParams();
    documentTypes.forEach((type) => {
      params = params.append('documentTypes', type.toString());
    });

    return this.http.post<DocumentData[]>(`${this.apiUrl}/upload-multiple`, formData, {
      params,
    });
  }

  uploadSingleDocument(file: File, documentTypeId: number): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);

    const params = new HttpParams().set('documentType', documentTypeId.toString());

    return this.http.post(`${this.apiUrl}/upload-single`, formData, {
      params,
      responseType: 'text',
    });
  }

  getUploadedDocuments(): Observable<DocumentData[]> {
    const userId = this.authService.currentUserValue?.id;
    if (!userId) {
      throw new Error('Cannot fetch documents: user is not authenticated');
    }

    return this.http.get<DocumentData[]>(`${this.apiUrl}/info/all/${userId}`);
  }

  getDocumentPreview(documentId: string | number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/${documentId}`, {
      responseType: 'blob',
    });
  }

  downloadDocument(documentId: string | number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/${documentId}`, {
      responseType: 'blob',
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
  }

  deleteDocument(documentId: string | number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/delete/${documentId}`);
  }

  /**
   * The student's expiring documents and when each expires (GH-498). Silent: the documents page
   * works without it, so a failure shows nothing rather than a snackbar.
   */
  getMyDocumentExpiries(): Observable<DocumentExpiry[]> {
    return this.http.get<DocumentExpiry[]>(`${this.apiUrl}/my/expiry`, {
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
  }
}

/**
 * Whether this is the backend's "no active application" refusal.
 *
 * <p>Narrows rather than casting: the argument arrives from an error callback, where nothing has
 * checked the shape, and `any` let the whole chain be dereferenced unchecked (GH-158).
 */
export function isNoActiveApplicationError(error: unknown): boolean {
  const message = (error as { error?: { message?: unknown } } | null)?.error?.message;
  return typeof message === 'string' && message.includes('No active application found');
}
