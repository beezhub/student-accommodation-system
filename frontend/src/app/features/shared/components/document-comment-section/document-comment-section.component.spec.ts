import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

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

  // What a tenant admin can now reach, and what they are told when the server says no.
  describe('as a tenant admin', () => {
    const existing = {
      id: 1,
      documentId: 5,
      adminName: 'An Admin',
      commentText: 'A comment',
      createdAt: new Date('2026-01-01'),
    };

    function setup() {
      const comments = jasmine.createSpyObj('DocumentCommentService', [
        'getComments',
        'addComment',
        'deleteComment',
      ]);
      comments.getComments.and.returnValue(of([existing]));
      const snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
      TestBed.configureTestingModule({
        imports: [DocumentCommentSectionComponent],
        providers: [
          provideNoopAnimations(),
          { provide: AuthService, useValue: { currentUserValue: { userRole: ROLE.TENANT_ADMIN } } },
          { provide: DocumentCommentService, useValue: comments },
        ],
      });
      // The component imports MatSnackBarModule itself, so the spy has to be an override.
      TestBed.overrideProvider(MatSnackBar, { useValue: snackBar });
      const fixture = TestBed.createComponent(DocumentCommentSectionComponent);
      fixture.componentRef.setInput('documentId', 5);
      fixture.detectChanges();
      return { component: fixture.componentInstance, comments, snackBar };
    }

    it('submitComment_validText_addsItToTheList', () => {
      const { component, comments } = setup();
      const added = { ...existing, id: 2, commentText: 'Please upload a clearer copy' };
      comments.addComment.and.returnValue(of(added));
      component.commentControl.setValue('Please upload a clearer copy');

      component.submitComment();

      expect(comments.addComment).toHaveBeenCalledWith(5, 'Please upload a clearer copy');
      expect(component.comments).toEqual([existing, added]);
      expect(component.isSubmitting).toBeFalse();
    });

    it('submitComment_serverRefuses_showsItsMessageAndKeepsTheText', () => {
      const { component, comments, snackBar } = setup();
      spyOn(console, 'error');
      comments.addComment.and.returnValue(
        throwError(() => ({ error: { message: 'Document not found' } }))
      );
      component.commentControl.setValue('Please upload a clearer copy');

      component.submitComment();

      expect(snackBar.open).toHaveBeenCalledWith('Document not found', 'Close', jasmine.anything());
      expect(component.commentControl.value).toBe('Please upload a clearer copy');
      expect(component.isSubmitting).toBeFalse();
    });

    it('deleteComment_success_removesItFromTheList', () => {
      const { component, comments } = setup();
      comments.deleteComment.and.returnValue(of(undefined));

      component.deleteComment(existing);

      expect(comments.deleteComment).toHaveBeenCalledWith(1);
      expect(component.comments).toEqual([]);
    });

    it('deleteComment_serverRefuses_keepsItInTheList', () => {
      const { component, comments, snackBar } = setup();
      spyOn(console, 'error');
      comments.deleteComment.and.returnValue(throwError(() => new Error('403')));

      component.deleteComment(existing);

      expect(component.comments).toEqual([existing]);
      expect(snackBar.open).toHaveBeenCalledWith(
        'Failed to delete comment',
        'Close',
        jasmine.anything()
      );
    });
  });
});
