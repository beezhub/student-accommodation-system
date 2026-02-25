import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import {
  InstitutionService,
  InstitutionResponse,
  InstitutionRequest,
  PageResponse,
} from './institution.service';
import { environment } from '../../../environments/environment';
import { Institution } from '../models/institution.model';

describe('InstitutionService', () => {
  let service: InstitutionService;
  let httpMock: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/institution`;

  const mockInstitution: Institution = {
    id: 1,
    name: 'Test University',
    address: '123 Main St',
    phoneNumber: '555-0100',
    emailAddress: 'admin@test.edu',
  };

  const mockResponse: InstitutionResponse = {
    id: 1,
    name: 'Test University',
    address: '123 Main St',
    phoneNumber: '555-0100',
    emailAddress: 'admin@test.edu',
    createdAt: '2026-01-01',
    updatedAt: '2026-01-01',
  };

  const mockPage: PageResponse<InstitutionResponse> = {
    content: [mockResponse],
    pageNumber: 0,
    pageSize: 20,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
    empty: false,
  };

  const mockRequest: InstitutionRequest = {
    name: 'New University',
    address: '456 Elm St',
    phoneNumber: '555-0200',
    emailAddress: 'new@uni.edu',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [InstitutionService],
    });
    service = TestBed.inject(InstitutionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getInstitutions', () => {
    it('getInstitutions_firstCall_fetchesFromApi', () => {
      let result: Institution[] | undefined;

      service.getInstitutions().subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/all`);
      expect(req.request.method).toBe('GET');
      req.flush([mockInstitution]);

      expect(result).toEqual([mockInstitution]);
    });

    it('getInstitutions_secondCall_returnsCachedResult', () => {
      // First call populates cache
      service.getInstitutions().subscribe();
      httpMock.expectOne(`${apiUrl}/all`).flush([mockInstitution]);

      // Second call should return cache — no HTTP request
      let result: Institution[] | undefined;
      service.getInstitutions().subscribe((r) => (result = r));
      httpMock.expectNone(`${apiUrl}/all`);

      expect(result).toEqual([mockInstitution]);
    });

    it('getInstitutions_afterClearCache_refetchesFromApi', () => {
      // Populate cache
      service.getInstitutions().subscribe();
      httpMock.expectOne(`${apiUrl}/all`).flush([mockInstitution]);

      // Clear cache
      service.clearCache();

      // Next call should hit the API again
      service.getInstitutions().subscribe();
      const req = httpMock.expectOne(`${apiUrl}/all`);
      req.flush([mockInstitution]);
    });
  });

  describe('getAllInstitutions', () => {
    it('getAllInstitutions_defaultFilters_usesDefaultPageAndSize', () => {
      service.getAllInstitutions({}).subscribe();

      const req = httpMock.expectOne((r) => r.url === apiUrl);
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      req.flush(mockPage);
    });

    it('getAllInstitutions_customFilters_sendsCorrectParams', () => {
      service.getAllInstitutions({ page: 2, size: 10 }).subscribe();

      const req = httpMock.expectOne((r) => r.url === apiUrl);
      expect(req.request.params.get('page')).toBe('2');
      expect(req.request.params.get('size')).toBe('10');
      req.flush(mockPage);
    });
  });

  describe('searchInstitutions', () => {
    it('searchInstitutions_noFilters_usesDefaultPageParams', () => {
      service.searchInstitutions({}).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      expect(req.request.params.has('name')).toBeFalse();
      req.flush(mockPage);
    });

    it('searchInstitutions_withName_appendsNameParam', () => {
      service.searchInstitutions({ name: 'Test' }).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.get('name')).toBe('Test');
      req.flush(mockPage);
    });

    it('searchInstitutions_withAddress_appendsAddressParam', () => {
      service.searchInstitutions({ address: '123 Main' }).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.get('address')).toBe('123 Main');
      req.flush(mockPage);
    });

    it('searchInstitutions_withContactEmail_appendsEmailParam', () => {
      service.searchInstitutions({ contactEmail: 'test@edu' }).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.get('contactEmail')).toBe('test@edu');
      req.flush(mockPage);
    });

    it('searchInstitutions_withAllFilters_appendsAllParams', () => {
      service.searchInstitutions({ name: 'Uni', address: '1 St', contactEmail: 'a@b.com', page: 1, size: 5 }).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.get('name')).toBe('Uni');
      expect(req.request.params.get('address')).toBe('1 St');
      expect(req.request.params.get('contactEmail')).toBe('a@b.com');
      expect(req.request.params.get('page')).toBe('1');
      expect(req.request.params.get('size')).toBe('5');
      req.flush(mockPage);
    });
  });

  describe('getInstitutionById', () => {
    it('getInstitutionById_validId_getsFromIdEndpoint', () => {
      let result: InstitutionResponse | undefined;

      service.getInstitutionById(1).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);

      expect(result).toEqual(mockResponse);
    });
  });

  describe('createInstitution', () => {
    it('createInstitution_validRequest_postsToBaseUrl', () => {
      let result: InstitutionResponse | undefined;

      service.createInstitution(mockRequest).subscribe((r) => (result = r));

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockRequest);
      req.flush(mockResponse);

      expect(result).toEqual(mockResponse);
    });

    it('createInstitution_clearsCacheOnCall', () => {
      // Pre-populate cache
      service.getInstitutions().subscribe();
      httpMock.expectOne(`${apiUrl}/all`).flush([mockInstitution]);

      // Create clears cache
      service.createInstitution(mockRequest).subscribe();
      httpMock.expectOne(apiUrl).flush(mockResponse);

      // Next getInstitutions should hit the API again
      service.getInstitutions().subscribe();
      httpMock.expectOne(`${apiUrl}/all`).flush([mockInstitution]);
    });
  });

  describe('updateInstitution', () => {
    it('updateInstitution_validRequest_putsToIdEndpoint', () => {
      let result: InstitutionResponse | undefined;

      service.updateInstitution(1, mockRequest).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(mockRequest);
      req.flush(mockResponse);

      expect(result).toEqual(mockResponse);
    });

    it('updateInstitution_clearsCacheOnCall', () => {
      // Pre-populate cache
      service.getInstitutions().subscribe();
      httpMock.expectOne(`${apiUrl}/all`).flush([mockInstitution]);

      // Update clears cache
      service.updateInstitution(1, mockRequest).subscribe();
      httpMock.expectOne(`${apiUrl}/1`).flush(mockResponse);

      // Next getInstitutions should hit the API again
      service.getInstitutions().subscribe();
      httpMock.expectOne(`${apiUrl}/all`).flush([mockInstitution]);
    });
  });

  describe('deleteInstitution', () => {
    it('deleteInstitution_validId_deletesToIdEndpoint', () => {
      service.deleteInstitution(1).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('deleteInstitution_clearsCacheOnCall', () => {
      // Pre-populate cache
      service.getInstitutions().subscribe();
      httpMock.expectOne(`${apiUrl}/all`).flush([mockInstitution]);

      // Delete clears cache
      service.deleteInstitution(1).subscribe();
      httpMock.expectOne(`${apiUrl}/1`).flush(null);

      // Next getInstitutions should hit the API again
      service.getInstitutions().subscribe();
      httpMock.expectOne(`${apiUrl}/all`).flush([mockInstitution]);
    });
  });

  describe('clearCache', () => {
    it('clearCache_setsInstitutionsToNull', () => {
      // Populate cache
      service.getInstitutions().subscribe();
      httpMock.expectOne(`${apiUrl}/all`).flush([mockInstitution]);

      service.clearCache();

      // Next call should fetch from API
      service.getInstitutions().subscribe();
      httpMock.expectOne(`${apiUrl}/all`).flush([mockInstitution]);
    });
  });
});
