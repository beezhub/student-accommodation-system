import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { ApplyOptionsService } from './apply-options.service';
import { BuildingResponse } from './building.service';

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

  it('getBuildings_relativeImageUrls_arePrefixedWithApiUrl', () => {
    const relative = '/building/12/images/3/content';
    const building: Partial<BuildingResponse> = {
      id: 12,
      name: 'Residence 1',
      primaryImageUrl: relative,
      images: [
        {
          id: 3,
          buildingId: 12,
          fileName: 'front.jpg',
          imageUrl: relative,
          isPrimary: true,
          displayOrder: 0,
          createdAt: '2026-01-01T00:00:00',
        },
      ],
    };
    let result: BuildingResponse[] = [];

    service.getBuildings(4).subscribe((buildings) => (result = buildings));
    httpMock.expectOne(`${apiUrl}/buildings?institutionId=4`).flush([building]);

    expect(result[0].primaryImageUrl).toBe(`${environment.apiUrl}${relative}`);
    expect(result[0].images?.[0].imageUrl).toBe(`${environment.apiUrl}${relative}`);
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
