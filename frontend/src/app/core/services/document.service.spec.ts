import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpEventType } from '@angular/common/http';
import {
  DocumentService,
  FileValidationResult,
  isNoActiveApplicationError,
  UploadProgress,
} from './document.service';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { DocumentData } from '../models/document.model';

describe('DocumentService', () => {
  let service: DocumentService;
  let httpMock: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  const apiUrl = `${environment.apiUrl}/documents`;

  const mockDoc: DocumentData = {
    id: 1,
    documentName: 'id.pdf',
    documentTypeName: 'National ID',
    documentStatus: 'PENDING',
    uploadDate: '2026-01-01',
  };

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [], {
      currentUserValue: {
        id: '42',
        firstName: 'J',
        lastName: 'D',
        email: 'j@d.com',
        profileCompleted: true,
        userRole: 'Student',
      },
    });

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DocumentService, { provide: AuthService, useValue: authServiceSpy }],
    });

    service = TestBed.inject(DocumentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('validateFile', () => {
    function makeFile(name: string, type: string, size: number): File {
      const blob = new Blob(['x'.repeat(size)], { type });
      return new File([blob], name, { type });
    }

    it('validateFile_validPdf_returnsValidTrue', () => {
      const file = makeFile('doc.pdf', 'application/pdf', 1024);
      const result: FileValidationResult = service.validateFile(file);
      expect(result.valid).toBeTrue();
      expect(result.errors.length).toBe(0);
    });

    it('validateFile_validJpeg_returnsValidTrue', () => {
      const file = makeFile('photo.jpg', 'image/jpeg', 1024);
      const result = service.validateFile(file);
      expect(result.valid).toBeTrue();
    });

    it('validateFile_validPng_returnsValidTrue', () => {
      const file = makeFile('photo.png', 'image/png', 1024);
      const result = service.validateFile(file);
      expect(result.valid).toBeTrue();
    });

    it('validateFile_oversizedFile_returnsError', () => {
      const file = makeFile('large.pdf', 'application/pdf', 6 * 1024 * 1024);
      const result = service.validateFile(file);
      expect(result.valid).toBeFalse();
      expect(result.errors.some((e) => e.includes('exceeds maximum'))).toBeTrue();
    });

    it('validateFile_wrongMimeType_returnsError', () => {
      const file = makeFile('doc.exe', 'application/octet-stream', 100);
      const result = service.validateFile(file);
      expect(result.valid).toBeFalse();
      expect(result.errors.some((e) => e.includes('not allowed'))).toBeTrue();
    });

    it('validateFile_emptyFile_returnsError', () => {
      const file = makeFile('empty.pdf', 'application/pdf', 0);
      const result = service.validateFile(file);
      expect(result.valid).toBeFalse();
      expect(result.errors.some((e) => e.includes('empty'))).toBeTrue();
    });
  });

  describe('uploadSingleDocument', () => {
    it('uploadSingleDocument_validFile_postsToUploadSingleEndpoint', () => {
      const file = new File(['content'], 'id.pdf', { type: 'application/pdf' });
      let result: string | undefined;

      service.uploadSingleDocument(file, 1).subscribe((r) => (result = r));

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/upload-single`);
      expect(req.request.method).toBe('POST');
      expect(req.request.params.get('documentType')).toBe('1');
      req.flush('upload-success', { status: 200, statusText: 'OK' });

      expect(result).toBe('upload-success');
    });
  });

  describe('uploadMultipleDocuments', () => {
    it('uploadMultipleDocuments_multipleFiles_postsToUploadMultipleEndpoint', () => {
      const files = [
        new File(['a'], 'a.pdf', { type: 'application/pdf' }),
        new File(['b'], 'b.jpg', { type: 'image/jpeg' }),
      ];

      service.uploadMultipleDocuments(files, [1, 2]).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/upload-multiple`);
      expect(req.request.method).toBe('POST');
      expect(req.request.params.getAll('documentTypes')).toEqual(['1', '2']);
      req.flush({ uploaded: 2 });
    });
  });

  describe('getUploadedDocuments', () => {
    it('getUploadedDocuments_authenticatedUser_fetchesDocumentsForUserId', () => {
      let result: DocumentData[] | undefined;

      service.getUploadedDocuments().subscribe((docs) => (result = docs));

      const req = httpMock.expectOne(`${apiUrl}/info/all/42`);
      expect(req.request.method).toBe('GET');
      req.flush([mockDoc]);

      expect(result).toEqual([mockDoc]);
    });
  });

  describe('getDocumentPreview', () => {
    it('getDocumentPreview_validId_getsFromDownloadEndpoint', () => {
      service.getDocumentPreview(1).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/download/1`);
      expect(req.request.method).toBe('GET');
      req.flush(new Blob());
    });
  });

  describe('isNoActiveApplicationError helper', () => {
    it('isNoActiveApplicationError_matchingMessage_returnsTrue', () => {
      const error = { error: { message: 'No active application found for this student' } };
      expect(isNoActiveApplicationError(error)).toBeTrue();
    });

    it('isNoActiveApplicationError_unrelatedMessage_returnsFalse', () => {
      const error = { error: { message: 'Something else went wrong' } };
      expect(isNoActiveApplicationError(error)).toBeFalse();
    });

    it('isNoActiveApplicationError_nullError_returnsFalse', () => {
      expect(isNoActiveApplicationError(null)).toBeFalse();
    });
  });

  describe('formatFileSize', () => {
    it('formatFileSize_zeroByes_returnsZeroBytes', () => {
      expect(service.formatFileSize(0)).toBe('0 Bytes');
    });

    it('formatFileSize_bytes_returnsInBytes', () => {
      expect(service.formatFileSize(512)).toBe('512 Bytes');
    });

    it('formatFileSize_kilobytes_returnsInKB', () => {
      expect(service.formatFileSize(1024)).toBe('1 KB');
    });

    it('formatFileSize_megabytes_returnsInMB', () => {
      expect(service.formatFileSize(1024 * 1024)).toBe('1 MB');
    });
  });

  describe('downloadDocument', () => {
    it('downloadDocument_validId_getsFromDownloadEndpoint', () => {
      service.downloadDocument(5).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/download/5`);
      expect(req.request.method).toBe('GET');
      req.flush(new Blob());
    });
  });

  describe('uploadSingleDocumentWithProgress', () => {
    it('uploadSingleDocumentWithProgress_postsToUploadSingleEndpoint', () => {
      const file = new File(['content'], 'id.pdf', { type: 'application/pdf' });
      const results: UploadProgress[] = [];

      service.uploadSingleDocumentWithProgress(file, 1).subscribe((r) => results.push(r));

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/upload-single`);
      expect(req.request.method).toBe('POST');
      expect(req.request.params.get('documentType')).toBe('1');

      // Simulate upload progress then response
      req.event({ type: HttpEventType.UploadProgress, loaded: 50, total: 100 });
      req.event({
        type: HttpEventType.Response,
        status: 200,
        statusText: 'OK',
        headers: req.request.headers,
        url: req.request.url,
        body: 'done',
      } as any);

      expect(results.length).toBeGreaterThan(0);
      expect(results.some((r) => r.status === 'progress')).toBeTrue();
    });
  });

  // eslint-disable-next-line @typescript-eslint/no-explicit-any

  describe('uploadMultipleDocumentsWithProgress', () => {
    it('uploadMultipleDocumentsWithProgress_postsToUploadMultipleEndpoint', () => {
      const files = [
        new File(['a'], 'a.pdf', { type: 'application/pdf' }),
        new File(['b'], 'b.jpg', { type: 'image/jpeg' }),
      ];

      service.uploadMultipleDocumentsWithProgress(files, [1, 2]).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/upload-multiple`);
      expect(req.request.method).toBe('POST');
      expect(req.request.params.getAll('documentTypes')).toEqual(['1', '2']);

      req.event({
        type: HttpEventType.Response,
        status: 200,
        statusText: 'OK',
        headers: req.request.headers,
        url: req.request.url,
        body: {},
      } as any);
    });
  });
});
