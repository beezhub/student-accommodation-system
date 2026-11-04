import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { BuildingAttributesService } from './building-attributes.service';
import { environment } from '../../../environments/environment';

describe('BuildingAttributesService', () => {
  const url = `${environment.apiUrl}/admin/buildings/7/attributes`;
  let service: BuildingAttributesService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BuildingAttributesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getBuildingAttributes_leavesTheErrorToTheDialog', () => {
    service.getBuildingAttributes(7).subscribe();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('GET');
    expect(req.request.headers.get('X-Silent-Error')).toBe('true');
    req.flush({});
  });

  /**
   * The backend stores a JSON list and needs buildingId in the body. Sending nothing else leaves the
   * deprecated gender and desirability values as they are: it skips fields that arrive as null.
   */
  it('saveAccessibilityFeatures_sendsOnlyTheBuildingAndAJsonList', () => {
    service.saveAccessibilityFeatures(7, ['Ramp access', 'Lift']).subscribe();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      buildingId: 7,
      accessibilityFeatures: '["Ramp access","Lift"]',
    });
    expect(req.request.headers.get('X-Silent-Error')).toBe('true');
    req.flush({});
  });

  it('saveAccessibilityFeatures_noFeatures_sendsAnEmptyListSoTheyAreCleared', () => {
    service.saveAccessibilityFeatures(7, []).subscribe();

    const req = http.expectOne(url);
    expect(req.request.body.accessibilityFeatures).toBe('[]');
    req.flush({});
  });
});
