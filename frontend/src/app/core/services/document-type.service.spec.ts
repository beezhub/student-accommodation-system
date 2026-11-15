import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { DocumentTypeService } from './document-type.service';
import { environment } from '../../../environments/environment';

/** #443: the student's Documents page asks for its own list, with the buildings picked on a form. */
describe('DocumentTypeService.getMyDocumentTypes', () => {
  const url = `${environment.apiUrl}/document-type/my`;
  let http: HttpTestingController;
  let service: DocumentTypeService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(DocumentTypeService);
  });

  afterEach(() => http.verify());

  it('getMyDocumentTypes_buildings_sendsThemInOneParameter', () => {
    service.getMyDocumentTypes([5, 6]).subscribe();

    const req = http.expectOne((r) => r.url === url);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('buildingIds')).toBe('5,6');
    req.flush([]);
  });

  it('getMyDocumentTypes_noBuildings_sendsNoParameter', () => {
    service.getMyDocumentTypes().subscribe();

    const req = http.expectOne((r) => r.url === url);
    expect(req.request.params.has('buildingIds')).toBeFalse();
    req.flush([]);
  });
});
