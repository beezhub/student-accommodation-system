import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { WaitlistService, WaitlistPosition } from './waitlist.service';
import { environment } from '../../../environments/environment';

describe('WaitlistService', () => {
  let service: WaitlistService;
  let httpMock: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/waitlist`;

  const mockPosition: WaitlistPosition = {
    position: 3,
    totalInQueue: 12,
    estimatedWaitDays: 6,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [WaitlistService],
    });
    service = TestBed.inject(WaitlistService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getMyPosition', () => {
    it('getMyPosition_studentOnWaitlist_returnsPosition', () => {
      let result: WaitlistPosition | null | undefined;

      service.getMyPosition().subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/position`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPosition);

      expect(result).toEqual(mockPosition);
    });

    it('getMyPosition_anyCall_sendsSilentErrorHeader', () => {
      // The 404 for a student with no WAITING entry is handled here, so errorInterceptor
      // must not also show its snackbar for it.
      service.getMyPosition().subscribe();

      const req = httpMock.expectOne(`${apiUrl}/position`);
      expect(req.request.headers.get('X-Silent-Error')).toBe('true');
      req.flush(mockPosition);
    });

    it('getMyPosition_studentNotOnWaitlist_returnsNullOn404', () => {
      let result: WaitlistPosition | null | undefined;

      service.getMyPosition().subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/position`);
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });

      expect(result).toBeNull();
    });

    it('getMyPosition_serverError_returnsNullRatherThanThrowing', () => {
      let result: WaitlistPosition | null | undefined;
      let errored = false;

      service.getMyPosition().subscribe({
        next: (r) => (result = r),
        error: () => (errored = true),
      });

      const req = httpMock.expectOne(`${apiUrl}/position`);
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });

      expect(errored).toBeFalse();
      expect(result).toBeNull();
    });
  });
});
