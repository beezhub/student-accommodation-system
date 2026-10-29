import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AddressSuggestion, GeocodingService, ResolvedPlace } from './geocoding.service';
import { environment } from '../../../environments/environment';

describe('GeocodingService', () => {
  let service: GeocodingService;
  let httpMock: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/geocoding`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [GeocodingService],
    });
    service = TestBed.inject(GeocodingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('search', () => {
    it('makes no request for a query too short to be an address', () => {
      let result: AddressSuggestion[] | undefined;
      service.search(' ab ').subscribe((r) => (result = r));

      expect(result).toEqual([]);
      httpMock.expectNone(() => true);
    });

    it('sends the trimmed query, the session and the silent-error header', () => {
      const suggestions: AddressSuggestion[] = [{ placeId: 'EiQz', displayName: '32 Tsutsube' }];
      let result: AddressSuggestion[] | undefined;
      service.search(' 32 Tsutsube ', 'session-1').subscribe((r) => (result = r));

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.get('q')).toBe('32 Tsutsube');
      expect(req.request.params.get('session')).toBe('session-1');
      expect(req.request.headers.get('X-Silent-Error')).toBe('true');
      req.flush(suggestions);

      expect(result).toEqual(suggestions);
    });

    it('leaves the session out when there is none', () => {
      service.search('1 Main Road').subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.has('session')).toBeFalse();
      req.flush([]);
    });

    it('turns a failure into no suggestions', () => {
      let result: AddressSuggestion[] | undefined;
      service.search('1 Main Road').subscribe((r) => (result = r));

      httpMock
        .expectOne((r) => r.url === `${apiUrl}/search`)
        .flush(null, { status: 503, statusText: 'Service Unavailable' });

      expect(result).toEqual([]);
    });
  });

  describe('resolve', () => {
    it('fetches the place with the session', () => {
      const place: ResolvedPlace = { placeId: 'EiQz', latitude: -26.25, longitude: 27.85 };
      let result: ResolvedPlace | null | undefined;
      service.resolve('EiQz', 'session-1').subscribe((r) => (result = r));

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/places/EiQz`);
      expect(req.request.params.get('session')).toBe('session-1');
      req.flush(place);

      expect(result).toEqual(place);
    });

    it('leaves the session out when there is none', () => {
      service.resolve('EiQz').subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/places/EiQz`);
      expect(req.request.params.has('session')).toBeFalse();
      req.flush({ placeId: 'EiQz', latitude: 1, longitude: 2 });
    });

    it('turns a failure into null', () => {
      let result: ResolvedPlace | null | undefined;
      service.resolve('EiQz').subscribe((r) => (result = r));

      httpMock
        .expectOne((r) => r.url === `${apiUrl}/places/EiQz`)
        .flush(null, { status: 503, statusText: 'Service Unavailable' });

      expect(result).toBeNull();
    });
  });

  describe('isEnabled', () => {
    it('is true only when maps-config says enabled', () => {
      let result: boolean | undefined;
      service.isEnabled().subscribe((r) => (result = r));

      httpMock.expectOne(`${environment.apiUrl}/building/maps-config`).flush({ enabled: true });

      expect(result).toBeTrue();
    });

    it('is false when maps-config fails', () => {
      let result: boolean | undefined;
      service.isEnabled().subscribe((r) => (result = r));

      httpMock
        .expectOne(`${environment.apiUrl}/building/maps-config`)
        .flush(null, { status: 500, statusText: 'Server Error' });

      expect(result).toBeFalse();
    });
  });

  it('makes a session token per pick', () => {
    const first = service.newSessionToken();

    expect(first).toBeTruthy();
    expect(service.newSessionToken()).not.toBe(first);
  });
});
