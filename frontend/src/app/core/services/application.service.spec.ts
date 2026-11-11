import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import {
  ApplicationService,
  ReviewProgressRequest,
  ApplicationFullDetails,
  ApplicationDocument,
} from './application.service';
import { environment } from '../../../environments/environment';
import { Application } from '../models/application.model';

describe('ApplicationService', () => {
  let service: ApplicationService;
  let httpMock: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/applications`;

  const mockApplication: Application = {
    id: 1,
    applicationCode: 'APP-001',
    status: 'PENDING',
    applicationDate: '2026-01-01',
    createdAt: '2026-01-01',
    updatedAt: '2026-01-01',
  } as Application;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApplicationService],
    });
    service = TestBed.inject(ApplicationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('createApplication', () => {
    it('createApplication_validData_postsToSubmitEndpoint', () => {
      const data = { preferredBuildingIds: [5], intakeIds: { 10: 3 } };
      let result: Application[] | undefined;

      service.createApplication(data).subscribe((apps) => (result = apps));

      const req = httpMock.expectOne(`${apiUrl}/submit`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(data);
      req.flush([mockApplication]);

      // GH-495: one application per company.
      expect(result).toEqual([mockApplication]);
    });
  });

  describe('getMyApplications', () => {
    it('getMyApplications_returnsListOfApplications', () => {
      let result: Application[] | undefined;

      service.getMyApplications().subscribe((apps) => (result = apps));

      const req = httpMock.expectOne(`${apiUrl}/my-applications`);
      expect(req.request.method).toBe('GET');
      req.flush([mockApplication]);

      expect(result).toEqual([mockApplication]);
    });
  });

  describe('getApplicationById', () => {
    it('getApplicationById_validId_returnsApplication', () => {
      let result: Application | undefined;

      service.getApplicationById(1).subscribe((app) => (result = app));

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockApplication);

      expect(result).toEqual(mockApplication);
    });
  });

  describe('hasActiveApplication', () => {
    it('hasActiveApplication_pendingApplication_returnsTrue', () => {
      let result: boolean | undefined;

      service.hasActiveApplication().subscribe((has) => (result = has));

      const req = httpMock.expectOne(`${apiUrl}/my-applications`);
      req.flush([{ ...mockApplication, status: 'PENDING' }]);

      expect(result).toBeTrue();
    });

    it('hasActiveApplication_approvedApplication_returnsTrue', () => {
      let result: boolean | undefined;

      service.hasActiveApplication().subscribe((has) => (result = has));

      const req = httpMock.expectOne(`${apiUrl}/my-applications`);
      req.flush([{ ...mockApplication, status: 'APPROVED' }]);

      expect(result).toBeTrue();
    });

    it('hasActiveApplication_noApplications_returnsFalse', () => {
      let result: boolean | undefined;

      service.hasActiveApplication().subscribe((has) => (result = has));

      const req = httpMock.expectOne(`${apiUrl}/my-applications`);
      req.flush([]);

      expect(result).toBeFalse();
    });

    it('hasActiveApplication_onlyRejectedApplications_returnsFalse', () => {
      let result: boolean | undefined;

      service.hasActiveApplication().subscribe((has) => (result = has));

      const req = httpMock.expectOne(`${apiUrl}/my-applications`);
      req.flush([{ ...mockApplication, status: 'REJECTED' }]);

      expect(result).toBeFalse();
    });

    it('hasActiveApplication_httpError_returnsFalse', () => {
      let result: boolean | undefined;

      service.hasActiveApplication().subscribe((has) => (result = has));

      const req = httpMock.expectOne(`${apiUrl}/my-applications`);
      req.error(new ProgressEvent('Network error'));

      expect(result).toBeFalse();
    });
  });

  describe('saveReviewProgress', () => {
    it('saveReviewProgress_validRequest_postsToCorrectEndpoint', () => {
      const request: ReviewProgressRequest = {
        applicationId: 1,
        documentUpdates: [{ documentId: 10, newStatus: 'APPROVED' }],
      };
      let result: unknown;

      service.saveReviewProgress(request).subscribe((r) => (result = r));
      const req = httpMock.expectOne(`${apiUrl}/1/save-review-progress`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush({ success: true });

      expect(result).toEqual({ success: true });
    });
  });

  describe('getApplicationByStatus', () => {
    it('getApplicationByStatus_statusParam_getsFromStatusEndpoint', () => {
      service.getApplicationByStatus('PENDING').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/status/PENDING`);
      expect(req.request.method).toBe('GET');
      req.flush([mockApplication]);
    });
  });

  describe('markUnderReview', () => {
    it('markUnderReview_validId_postsToMarkUnderReviewEndpoint', () => {
      service.markUnderReview(1).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/1/mark-under-review`);
      expect(req.request.method).toBe('POST');
      req.flush(mockApplication);
    });
  });

  describe('getActiveApplication', () => {
    it('getActiveApplication_pendingApp_returnsApp', () => {
      let result: Application | null | undefined;

      service.getActiveApplication().subscribe((app) => (result = app));

      const req = httpMock.expectOne(`${apiUrl}/my-applications`);
      req.flush([{ ...mockApplication, status: 'PENDING' }]);

      expect(result).not.toBeNull();
      expect(result?.status).toBe('PENDING');
    });

    it('getActiveApplication_noActiveApp_returnsNull', () => {
      let result: Application | null | undefined;

      service.getActiveApplication().subscribe((app) => (result = app));

      const req = httpMock.expectOne(`${apiUrl}/my-applications`);
      req.flush([{ ...mockApplication, status: 'REJECTED' }]);

      expect(result).toBeNull();
    });

    it('getActiveApplication_httpError_returnsNull', () => {
      let result: Application | null | undefined;

      service.getActiveApplication().subscribe((app) => (result = app));

      const req = httpMock.expectOne(`${apiUrl}/my-applications`);
      req.error(new ProgressEvent('Network error'));

      expect(result).toBeNull();
    });
  });

  describe('getApplicationFullDetails', () => {
    it('getApplicationFullDetails_validId_getsFromFullDetailsEndpoint', () => {
      const mockDetails = { id: 1, applicationCode: 'APP-001' };
      service.getApplicationFullDetails(1).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/1/full-details`);
      expect(req.request.method).toBe('GET');
      req.flush(mockDetails);
    });
  });

  describe('getApplicationDocuments', () => {
    it('getApplicationDocuments_validId_mapsDocumentsFromFullDetails', () => {
      const fullDetails: Partial<ApplicationFullDetails> = {
        id: 1,
        applicationCode: 'APP-001',
        status: 'PENDING',
        applicationDate: '2026-01-01',
        createdAt: '2026-01-01',
        updatedAt: '2026-01-01',
        studentProfile: {
          id: 1,
          studentNumber: 'S001',
          firstName: 'Jane',
          lastName: 'Doe',
          email: 'jane@test.com',
        },
        documents: [
          {
            id: 10,
            fileName: 'id.pdf',
            type: 'National ID',
            status: 'APPROVED',
            uploadedAt: '2026-01-01',
            documentTypeId: 1,
            documentTypeName: 'National ID',
            comments: [],
          },
        ],
        requiredDocumentsChecklist: {
          applicationId: 1,
          totalRequired: 1,
          approved: 1,
          pending: 0,
          rejected: 0,
          notUploaded: 0,
          documents: [],
          allApproved: true,
        },
        allocationEligibility: {
          eligible: true,
          applicationApproved: true,
          allDocumentsApproved: true,
        },
      };

      let result: ApplicationDocument[] = [];
      service.getApplicationDocuments(1).subscribe((docs) => (result = docs));

      const req = httpMock.expectOne(`${apiUrl}/1/full-details`);
      req.flush(fullDetails);
      expect(result.length).toBe(1);
      expect(result[0].documentName).toBe('id.pdf');
      expect(result[0].documentStatus).toBe('APPROVED');
    });
  });

  describe('moveToAllocationPhase', () => {
    it('moveToAllocationPhase_validId_postsToMoveToAllocationEndpoint', () => {
      service.moveToAllocationPhase(1).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/1/move-to-allocation`);
      expect(req.request.method).toBe('POST');
      req.flush(mockApplication);
    });
  });

  describe('requestChanges', () => {
    it('requestChanges_validIdAndReason_postsToRequestChangesEndpoint', () => {
      service.requestChanges(1, 'Missing document').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/1/request-changes`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ reason: 'Missing document' });
      req.flush(mockApplication);
    });
  });

  describe('resubmitApplication', () => {
    it('resubmitApplication_validId_postsToResubmitEndpoint', () => {
      service.resubmitApplication(1).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/1/resubmit`);
      expect(req.request.method).toBe('POST');
      req.flush(mockApplication);
    });
  });
});
