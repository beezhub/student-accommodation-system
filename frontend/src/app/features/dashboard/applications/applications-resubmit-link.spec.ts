import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { ApplicationsComponent } from './applications.component';
import { ApplicationService } from '../../../core/services/application.service';
import { Application } from '../../../core/models/application.model';
import { AuthService } from '../../../core/services/auth.service';
import { DocumentService } from '../../../core/services/document.service';
import { DocumentTypeService } from '../../../core/services/document-type.service';

describe('ApplicationsComponent resubmit link', () => {
  function resubmitLinks(status: string): HTMLButtonElement[] {
    TestBed.configureTestingModule({
      imports: [ApplicationsComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        {
          provide: ApplicationService,
          useValue: { getMyApplications: () => of([{ id: 7, status } as Application]) },
        },
        { provide: AuthService, useValue: { currentUserValue: null } },
        { provide: DocumentService, useValue: { getUploadedDocuments: () => of([]) } },
        { provide: DocumentTypeService, useValue: { getRequiredDocumentTypes: () => of([]) } },
      ],
    });
    const fixture = TestBed.createComponent(ApplicationsComponent);
    fixture.detectChanges();
    const buttons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('button')
    );
    return buttons.filter((b) => (b.textContent ?? '').includes('Review and Resubmit'));
  }

  it('list_changesRequested_pointsTheStudentAtResubmit', () => {
    // One for the mobile cards and one for the desktop table; CSS shows one of them.
    expect(resubmitLinks('CHANGES_REQUESTED').length).toBe(2);
  });

  it('list_otherStatus_hasNoResubmitLink', () => {
    expect(resubmitLinks('PENDING').length).toBe(0);
  });
});
