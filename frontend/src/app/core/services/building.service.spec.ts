import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BuildingService, BuildingImageResponse, BuildingResponse } from './building.service';
import { PageResponse } from '../models/page-response.model';
import { environment } from '../../../environments/environment';

describe('BuildingService', () => {
  let service: BuildingService;
  let httpMock: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/building`;

  const image: BuildingImageResponse = {
    id: 1,
    buildingId: 5,
    fileName: 'front.jpg',
    imageUrl: '/building/5/images/1/content',
    isPrimary: true,
    displayOrder: 0,
    createdAt: '2026-01-01T00:00:00',
  };

  const building: BuildingResponse = {
    id: 5,
    name: 'Some Residence',
    primaryImageUrl: '/building/5/images/1/content',
    images: [image],
    createdAt: '2026-01-01T00:00:00',
    updatedAt: '2026-01-01T00:00:00',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(BuildingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // GH-268: image URLs used to be presigned MinIO URLs the browser could never reach in a
  // deployed environment. The backend now returns a path relative to the API root, and this
  // service is where that gets resolved into something an <img> tag can actually load.

  it('getBuildingById resolves the primary image and each nested image URL', () => {
    service.getBuildingById(5).subscribe((response) => {
      expect(response.primaryImageUrl).toBe(`${environment.apiUrl}/building/5/images/1/content`);
      expect(response.images?.[0].imageUrl).toBe(
        `${environment.apiUrl}/building/5/images/1/content`
      );
    });

    const req = httpMock.expectOne(`${apiUrl}/5`);
    req.flush(building);
  });

  it('getBuildingImages resolves every image URL in the list', () => {
    service.getBuildingImages(5).subscribe((images) => {
      expect(images[0].imageUrl).toBe(`${environment.apiUrl}/building/5/images/1/content`);
    });

    const req = httpMock.expectOne(`${apiUrl}/5/images`);
    req.flush([image]);
  });

  it('getAllBuildings resolves image URLs on every item in a page', () => {
    const page: PageResponse<BuildingResponse> = {
      content: [building],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
    };

    service.getAllBuildings().subscribe((response) => {
      expect(response.content[0].primaryImageUrl).toBe(
        `${environment.apiUrl}/building/5/images/1/content`
      );
    });

    const req = httpMock.expectOne((r) => r.url === apiUrl);
    req.flush(page);
  });

  it('leaves a missing image URL as-is instead of building a broken absolute URL', () => {
    const buildingWithoutImage: BuildingResponse = {
      ...building,
      primaryImageUrl: undefined,
      images: [],
    };

    service.getBuildingById(5).subscribe((response) => {
      expect(response.primaryImageUrl).toBeUndefined();
    });

    const req = httpMock.expectOne(`${apiUrl}/5`);
    req.flush(buildingWithoutImage);
  });

  it('browseBuildingsPaginated resolves image URLs on every item in a page', () => {
    const page: PageResponse<BuildingResponse> = {
      content: [building],
      totalElements: 1,
      totalPages: 1,
      size: 12,
      number: 0,
    };

    service.browseBuildingsPaginated({}).subscribe((response) => {
      expect(response.content[0].primaryImageUrl).toBe(
        `${environment.apiUrl}/building/5/images/1/content`
      );
    });

    const req = httpMock.expectOne((r) => r.url === `${apiUrl}/browse`);
    req.flush(page);
  });

  it('searchBuildings resolves image URLs on every item in a page', () => {
    const page: PageResponse<BuildingResponse> = {
      content: [building],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
    };

    service.searchBuildings({}).subscribe((response) => {
      expect(response.content[0].primaryImageUrl).toBe(
        `${environment.apiUrl}/building/5/images/1/content`
      );
    });

    const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
    req.flush(page);
  });

  it('getAllBuildingsNoPagination resolves image URLs on every building', () => {
    service.getAllBuildingsNoPagination().subscribe((buildings) => {
      expect(buildings[0].primaryImageUrl).toBe(
        `${environment.apiUrl}/building/5/images/1/content`
      );
    });

    const req = httpMock.expectOne(`${apiUrl}/all`);
    req.flush([building]);
  });

  it('getBuildingsByInstitution resolves image URLs on every building', () => {
    service.getBuildingsByInstitution(9).subscribe((buildings) => {
      expect(buildings[0].primaryImageUrl).toBe(
        `${environment.apiUrl}/building/5/images/1/content`
      );
    });

    const req = httpMock.expectOne(`${apiUrl}/by-institution/9`);
    req.flush([building]);
  });

  it('createBuilding resolves the image URL on the created building', () => {
    service.createBuilding({ name: 'New Residence' }).subscribe((response) => {
      expect(response.primaryImageUrl).toBe(`${environment.apiUrl}/building/5/images/1/content`);
    });

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    req.flush(building);
  });

  it('updateBuilding resolves the image URL on the updated building', () => {
    service.updateBuilding(5, { name: 'Renamed' }).subscribe((response) => {
      expect(response.primaryImageUrl).toBe(`${environment.apiUrl}/building/5/images/1/content`);
    });

    const req = httpMock.expectOne(`${apiUrl}/5`);
    expect(req.request.method).toBe('PUT');
    req.flush(building);
  });

  it('uploadBuildingImages resolves every uploaded image URL', () => {
    service.uploadBuildingImages(5, []).subscribe((images) => {
      expect(images[0].imageUrl).toBe(`${environment.apiUrl}/building/5/images/1/content`);
    });

    const req = httpMock.expectOne(`${apiUrl}/5/images`);
    expect(req.request.method).toBe('POST');
    req.flush([image]);
  });

  it('setPrimaryImage resolves the image URL on the response', () => {
    service.setPrimaryImage(5, 1).subscribe((response) => {
      expect(response.imageUrl).toBe(`${environment.apiUrl}/building/5/images/1/content`);
    });

    const req = httpMock.expectOne(`${apiUrl}/5/images/1/primary`);
    expect(req.request.method).toBe('PUT');
    req.flush(image);
  });
});
