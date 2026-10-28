import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { TenantService } from './tenant.service';
import { environment } from '../../../environments/environment';

/**
 * GH-267: the two lists this service serves are not interchangeable.
 *
 * <p>The tenant administration screen is the only place a deactivated company can be switched back
 * on, so it must keep listing them. Every dropdown that assigns a building or a staff account to a
 * company must not offer them. Collapsing the two would make deactivation irreversible through the
 * UI, which is why the query parameter exists rather than a filter at the single call site.
 */
describe('TenantService', () => {
  let service: TenantService;
  let httpMock: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/admin/tenants`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TenantService],
    });
    service = TestBed.inject(TenantService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getAllTenants_sendsNoActiveOnlyParam_soInactiveTenantsStayListed', () => {
    service.getAllTenants().subscribe();

    const req = httpMock.expectOne((r) => r.url === apiUrl);
    expect(req.request.params.get('activeOnly')).toBeNull();
    req.flush([]);
  });

  it('getActiveTenants_asksForActiveOnly', () => {
    service.getActiveTenants().subscribe();

    const req = httpMock.expectOne((r) => r.url === apiUrl);
    expect(req.request.params.get('activeOnly')).toBe('true');
    req.flush([]);
  });

  it('moveTenantAdmin_postsTargetTenant_andSilencesTheInterceptorSnackbar', () => {
    service.moveTenantAdmin(1, 10, 5).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/1/admins/10/move`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ targetTenantId: 5 });
    // The move dialog shows the error inline, so the interceptor's snackbar would be a duplicate.
    expect(req.request.headers.has('X-Silent-Error')).toBeTrue();
    req.flush({});
  });
});
