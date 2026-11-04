import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { DocumentTypeManagementComponent } from './document-type-management.component';
import { DocumentTypeService } from '../../../core/services/document-type.service';
import { AuthService } from '../../../core/services/auth.service';
import { ROLE, UserRoleValue } from '../../../core/models/role.model';

describe('DocumentTypeManagementComponent row actions', () => {
  function renderAs(userRole: UserRoleValue, isPlatformDefault: boolean): HTMLElement {
    TestBed.configureTestingModule({
      imports: [DocumentTypeManagementComponent],
      providers: [
        provideNoopAnimations(),
        { provide: AuthService, useValue: { currentUserValue: { userRole } } },
        {
          provide: DocumentTypeService,
          useValue: jasmine.createSpyObj('DocumentTypeService', {
            searchDocumentTypes: of({
              content: [{ id: 3, name: 'ID Copy', isRequired: true, isPlatformDefault }],
              totalElements: 1,
            }),
          }),
        },
      ],
    });
    const fixture = TestBed.createComponent(DocumentTypeManagementComponent);
    fixture.detectChanges();
    return fixture.nativeElement;
  }

  it('platformDefaultRow_platformStaff_offersNoActions', () => {
    // guardPlatformType answers 403 to anyone but PLATFORM_ADMIN on these rows.
    expect(renderAs(ROLE.PLATFORM_STAFF, true).querySelector('.action-btn')).toBeNull();
  });

  it('platformDefaultRow_platformAdmin_offersEditToggleAndDelete', () => {
    const page = renderAs(ROLE.PLATFORM_ADMIN, true);

    expect(page.querySelector('.action-btn.edit')).not.toBeNull();
    expect(page.querySelector('.action-btn.toggle')).not.toBeNull();
    expect(page.querySelector('.action-btn.delete')).not.toBeNull();
  });

  it('platformDefaultRow_tenantAdmin_offersOnlyOverride', () => {
    const page = renderAs(ROLE.TENANT_ADMIN, true);

    expect(page.querySelector('.action-btn.override')).not.toBeNull();
    expect(page.querySelector('.action-btn.edit')).toBeNull();
  });

  it('ownRow_tenantAdmin_offersEditToggleAndDelete', () => {
    const page = renderAs(ROLE.TENANT_ADMIN, false);

    expect(page.querySelector('.action-btn.edit')).not.toBeNull();
    expect(page.querySelector('.action-btn.override')).toBeNull();
  });
});
