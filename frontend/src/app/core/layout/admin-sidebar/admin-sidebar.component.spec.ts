import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { AdminSidebarComponent } from './admin-sidebar.component';
import { AuthService } from '../../services/auth.service';
import { ROLE } from '../../models/role.model';
import { User } from '../../models/user.model';

describe('AdminSidebarComponent', () => {
  let component: AdminSidebarComponent;
  let fixture: ComponentFixture<AdminSidebarComponent>;
  let currentUserSubject: BehaviorSubject<User | null>;

  function createAuthServiceSpy(userRole: string | null) {
    currentUserSubject = new BehaviorSubject<User | null>(userRole ? ({ userRole } as User) : null);
    return jasmine.createSpyObj<AuthService>('AuthService', ['logout'], {
      currentUser: currentUserSubject.asObservable(),
      currentUserValue: userRole ? ({ userRole } as User) : null,
    });
  }

  async function createComponent(userRole: string | null) {
    const authServiceSpy = createAuthServiceSpy(userRole);
    await TestBed.configureTestingModule({
      imports: [AdminSidebarComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminSidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  describe('basic creation', () => {
    beforeEach(async () => {
      await createComponent(ROLE.PLATFORM_ADMIN);
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });
  });

  describe('sidebar nav items — PLATFORM_ADMIN', () => {
    beforeEach(async () => {
      await createComponent(ROLE.PLATFORM_ADMIN);
    });

    it('shows platform-only items (Tenants, Users, System Settings, Feature Flags, Activity Logs)', () => {
      const platformOnlyItems = component.navGroups
        .flatMap((g) => g.items)
        .filter((item) => item.platformAdminOnly);
      expect(platformOnlyItems.length).toBeGreaterThan(0);
    });

    it('isPlatformAdmin$ emits true for PLATFORM_ADMIN user', (done) => {
      component.isPlatformAdmin$.subscribe((val) => {
        expect(val).toBeTrue();
        done();
      });
    });
  });

  describe('sidebar nav items — TENANT_ADMIN', () => {
    beforeEach(async () => {
      await createComponent(ROLE.TENANT_ADMIN);
    });

    it('isPlatformAdmin$ emits false for TENANT_ADMIN user', (done) => {
      component.isPlatformAdmin$.subscribe((val) => {
        expect(val).toBeFalse();
        done();
      });
    });

    it('navGroups contain platformAdminOnly items that should be hidden', () => {
      const platformOnlyLabels = component.navGroups
        .flatMap((g) => g.items)
        .filter((item) => item.platformAdminOnly)
        .map((item) => item.label);

      expect(platformOnlyLabels).toContain('Tenants');
      expect(platformOnlyLabels).toContain('Users');
      expect(platformOnlyLabels).toContain('Feature Flags');
    });

    /**
     * These two were flagged platform-only while the backend @PreAuthorize'd both for
     * TENANT_ADMIN and the route dropped PlatformAdminGuard for exactly that reason (GH-158).
     * Hiding the links left them reachable only by typing the URL (GH-238).
     */
    it('navGroups expose the screens a tenant admin is actually allowed', () => {
      const visibleLabels = component.navGroups
        .flatMap((g) => g.items)
        .filter((item) => !item.platformAdminOnly)
        .map((item) => item.label);

      expect(visibleLabels).toContain('System Settings');
      expect(visibleLabels).toContain('Activity Logs');
    });

    it('non-platform-only items remain visible for TENANT_ADMIN', () => {
      const visibleItems = component.navGroups
        .flatMap((g) => g.items)
        .filter((item) => !item.platformAdminOnly);

      expect(visibleItems.some((item) => item.label === 'Dashboard')).toBeTrue();
      expect(visibleItems.some((item) => item.label === 'Applications')).toBeTrue();
      expect(visibleItems.some((item) => item.label === 'Buildings')).toBeTrue();
    });
  });
});
