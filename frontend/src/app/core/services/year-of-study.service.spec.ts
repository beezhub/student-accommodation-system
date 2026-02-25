import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import {
  YearOfStudyService,
  YearOfStudyResponse,
  YearOfStudyRequest,
  PageResponse,
} from './year-of-study.service';
import { environment } from '../../../environments/environment';
import { YearOfStudy } from '../models/year-of-study.model';

describe('YearOfStudyService', () => {
  let service: YearOfStudyService;
  let httpMock: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/year-of-study`;

  const mockYearOfStudy: YearOfStudy = {
    id: 1,
    yearOfStudy: '1st Year',
  };

  const mockResponse: YearOfStudyResponse = {
    id: 1,
    yearOfStudy: '1st Year',
  };

  const mockPage: PageResponse<YearOfStudyResponse> = {
    content: [mockResponse],
    pageNumber: 0,
    pageSize: 20,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
    empty: false,
  };

  const mockRequest: YearOfStudyRequest = {
    yearOfStudy: '5th Year',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [YearOfStudyService],
    });
    service = TestBed.inject(YearOfStudyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getYearsOfStudy', () => {
    it('getYearsOfStudy_firstCall_fetchesFromApi', () => {
      let result: YearOfStudy[] | undefined;

      service.getYearsOfStudy().subscribe((r) => (result = r));

      const req = httpMock.expectOne((r) => r.url === apiUrl);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('100');
      req.flush({ content: [mockYearOfStudy], pageNumber: 0, pageSize: 100, totalElements: 1, totalPages: 1, first: true, last: true, empty: false });

      expect(result).toEqual([mockYearOfStudy]);
    });

    it('getYearsOfStudy_secondCall_returnsCachedResult', () => {
      // First call populates cache
      service.getYearsOfStudy().subscribe();
      httpMock.expectOne((r) => r.url === apiUrl).flush({ content: [mockYearOfStudy], pageNumber: 0, pageSize: 100, totalElements: 1, totalPages: 1, first: true, last: true, empty: false });

      // Second call should return cache — no HTTP request
      let result: YearOfStudy[] | undefined;
      service.getYearsOfStudy().subscribe((r) => (result = r));
      httpMock.expectNone(apiUrl);

      expect(result).toEqual([mockYearOfStudy]);
    });

    it('getYearsOfStudy_afterClearCache_refetchesFromApi', () => {
      // Populate cache
      service.getYearsOfStudy().subscribe();
      httpMock.expectOne((r) => r.url === apiUrl).flush({ content: [mockYearOfStudy], pageNumber: 0, pageSize: 100, totalElements: 1, totalPages: 1, first: true, last: true, empty: false });

      // Clear cache
      service.clearCache();

      // Next call should hit the API
      service.getYearsOfStudy().subscribe();
      const req = httpMock.expectOne((r) => r.url === apiUrl);
      req.flush({ content: [mockYearOfStudy], pageNumber: 0, pageSize: 100, totalElements: 1, totalPages: 1, first: true, last: true, empty: false });
    });
  });

  describe('getAllYearsOfStudyPaginated', () => {
    it('getAllYearsOfStudyPaginated_defaultFilters_usesDefaultPageAndSize', () => {
      service.getAllYearsOfStudyPaginated({}).subscribe();

      const req = httpMock.expectOne((r) => r.url === apiUrl);
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      req.flush(mockPage);
    });

    it('getAllYearsOfStudyPaginated_customFilters_sendsCorrectParams', () => {
      service.getAllYearsOfStudyPaginated({ page: 3, size: 5 }).subscribe();

      const req = httpMock.expectOne((r) => r.url === apiUrl);
      expect(req.request.params.get('page')).toBe('3');
      expect(req.request.params.get('size')).toBe('5');
      req.flush(mockPage);
    });
  });

  describe('searchYearsOfStudy', () => {
    it('searchYearsOfStudy_noFilters_usesDefaultPageParams', () => {
      service.searchYearsOfStudy({}).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      expect(req.request.params.has('year')).toBeFalse();
      req.flush(mockPage);
    });

    it('searchYearsOfStudy_withYear_appendsYearParam', () => {
      service.searchYearsOfStudy({ year: 2 }).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.get('year')).toBe('2');
      req.flush(mockPage);
    });

    it('searchYearsOfStudy_withNullYear_omitsYearParam', () => {
      service.searchYearsOfStudy({ year: null }).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.has('year')).toBeFalse();
      req.flush(mockPage);
    });

    it('searchYearsOfStudy_withEmptyStringYear_omitsYearParam', () => {
      service.searchYearsOfStudy({ year: '' }).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.has('year')).toBeFalse();
      req.flush(mockPage);
    });

    it('searchYearsOfStudy_withCustomPage_sendsPageParam', () => {
      service.searchYearsOfStudy({ page: 2, size: 10 }).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.get('page')).toBe('2');
      expect(req.request.params.get('size')).toBe('10');
      req.flush(mockPage);
    });
  });

  describe('getYearOfStudyById', () => {
    it('getYearOfStudyById_validId_getsFromIdEndpoint', () => {
      let result: YearOfStudyResponse | undefined;

      service.getYearOfStudyById(1).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);

      expect(result).toEqual(mockResponse);
    });
  });

  describe('createYearOfStudy', () => {
    it('createYearOfStudy_validRequest_postsToBaseUrl', () => {
      let result: YearOfStudyResponse | undefined;

      service.createYearOfStudy(mockRequest).subscribe((r) => (result = r));

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockRequest);
      req.flush(mockResponse);

      expect(result).toEqual(mockResponse);
    });
  });

  describe('updateYearOfStudy', () => {
    it('updateYearOfStudy_validRequest_putsToIdEndpoint', () => {
      let result: YearOfStudyResponse | undefined;

      service.updateYearOfStudy(1, mockRequest).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(mockRequest);
      req.flush(mockResponse);

      expect(result).toEqual(mockResponse);
    });
  });

  describe('deleteYearOfStudy', () => {
    it('deleteYearOfStudy_validId_deletesToIdEndpoint', () => {
      service.deleteYearOfStudy(1).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('clearCache', () => {
    it('clearCache_setsYearsCacheToNull', () => {
      // Populate cache
      service.getYearsOfStudy().subscribe();
      httpMock.expectOne((r) => r.url === apiUrl).flush({ content: [mockYearOfStudy], pageNumber: 0, pageSize: 100, totalElements: 1, totalPages: 1, first: true, last: true, empty: false });

      service.clearCache();

      // Next call should hit API
      service.getYearsOfStudy().subscribe();
      const req = httpMock.expectOne((r) => r.url === apiUrl);
      req.flush({ content: [mockYearOfStudy], pageNumber: 0, pageSize: 100, totalElements: 1, totalPages: 1, first: true, last: true, empty: false });
    });
  });
});
