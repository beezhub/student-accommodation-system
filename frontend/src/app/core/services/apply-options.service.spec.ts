import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { ApplyOptionsService } from './apply-options.service';

describe('ApplyOptionsService', () => {
  const apiUrl = `${environment.apiUrl}/apply`;
  let service: ApplyOptionsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ApplyOptionsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getBuildings_withInstitution_sendsIt', () => {
    service.getBuildings(4).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/buildings?institutionId=4`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getBuildings_withoutInstitution_usesTheProfile', () => {
    service.getBuildings(null).subscribe();

    httpMock.expectOne(`${apiUrl}/buildings`).flush([]);
  });

  it('getOptions_asksForTheBuildingsCompany', () => {
    service.getOptions(7, 4).subscribe();

    httpMock.expectOne(`${apiUrl}/buildings/7/intakes?institutionId=4`).flush({});
  });

  it('getOpenWindows_isSilentOnError', () => {
    service.getOpenWindows().subscribe();

    const req = httpMock.expectOne(`${apiUrl}/open-windows`);
    expect(req.request.headers.get('X-Silent-Error')).toBe('true');
    req.flush([]);
  });
});
