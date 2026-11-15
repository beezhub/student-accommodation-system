import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { routes } from './app-routing.module';
import { PrivacyPolicyComponent } from './features/legal/privacy-policy/privacy-policy.component';
import { ContactInfoService } from './core/services/contact-info.service';
import { RoleService } from './core/services/role.service';

describe('routes', () => {
  describe('/privacy', () => {
    it('privacy_route_isPublicAndLazy', () => {
      const route = routes.find((r) => r.path === 'privacy');

      expect(route).toBeDefined();
      expect(route?.canActivate).toBeUndefined();
      expect(route?.canMatch).toBeUndefined();
      expect(route?.loadComponent).toBeDefined();
    });

    it('privacy_signedOutVisitor_rendersThePolicy', async () => {
      TestBed.configureTestingModule({
        providers: [
          provideRouter(routes),
          { provide: ContactInfoService, useValue: { getContactInfo: () => of(null) } },
          { provide: RoleService, useValue: { isStudent: () => false } },
        ],
      });

      const harness = await RouterTestingHarness.create();
      const policy = await harness.navigateByUrl('/privacy', PrivacyPolicyComponent);

      expect(policy).toBeInstanceOf(PrivacyPolicyComponent);
      expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain(
        'Privacy Policy'
      );
    });
  });
});
