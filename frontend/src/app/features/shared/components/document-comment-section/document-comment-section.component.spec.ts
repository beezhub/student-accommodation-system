import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { DocumentCommentSectionComponent } from './document-comment-section.component';
import { DocumentCommentService } from '../../../../core/services/document-comment.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ROLE, UserRoleValue } from '../../../../core/models/role.model';

describe('DocumentCommentSectionComponent', () => {
  function renderAs(userRole: UserRoleValue): HTMLElement {
    TestBed.configureTestingModule({
      imports: [DocumentCommentSectionComponent],
      providers: [
        provideNoopAnimations(),
        { provide: AuthService, useValue: { currentUserValue: { userRole } } },
        {
          provide: DocumentCommentService,
          useValue: jasmine.createSpyObj('DocumentCommentService', {
            getComments: of([
              {
                id: 1,
                documentId: 5,
                adminName: 'An Admin',
                commentText: 'A comment',
                createdAt: '2026-01-01',
              },
            ]),
          }),
        },
      ],
    });
    const fixture = TestBed.createComponent(DocumentCommentSectionComponent);
    fixture.componentRef.setInput('documentId', 5);
    fixture.detectChanges();
    return fixture.nativeElement;
  }

  // The roles DocumentController lets add and delete comments, once the hierarchy is applied.
  [ROLE.PLATFORM_ADMIN, ROLE.PLATFORM_STAFF, ROLE.TENANT_ADMIN].forEach((role) => {
    it(`commentActions_${role}_areOffered`, () => {
      const section = renderAs(role);

      expect(section.querySelector('.add-comment')).not.toBeNull();
      expect(section.querySelector('.delete-btn')).not.toBeNull();
    });
  });

  // BUILDING_STAFF is an admin to RoleService.isAdmin(), but the backend refuses it here.
  [ROLE.BUILDING_STAFF, ROLE.STUDENT].forEach((role) => {
    it(`commentActions_${role}_areNotOffered`, () => {
      const section = renderAs(role);

      expect(section.querySelector('.add-comment')).toBeNull();
      expect(section.querySelector('.delete-btn')).toBeNull();
    });
  });
});
