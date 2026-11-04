import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
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

  it('list_requiredDocuments_showWhichAreMissing', () => {
    TestBed.configureTestingModule({
      imports: [ApplicationsComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        {
          provide: ApplicationService,
          useValue: {
            getMyApplications: () =>
              of([
                { id: 1, status: 'PENDING', createdAt: '2026-01-01', tenantId: 4 } as Application,
                { id: 2, status: 'PENDING', applicationDate: '2026-02-01' } as Application,
              ]),
          },
        },
        { provide: AuthService, useValue: { currentUserValue: null } },
        {
          provide: DocumentService,
          useValue: { getUploadedDocuments: () => of([{ documentTypeName: 'ID' }]) },
        },
        {
          provide: DocumentTypeService,
          useValue: {
            getRequiredDocumentTypes: () =>
              of([
                { id: 10, name: 'ID' },
                { id: 11, name: 'Proof of registration' },
              ]),
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(ApplicationsComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component.applications.map((a) => a.id)).toEqual([2, 1]);
    expect(component.getDocRequirements(1).map((r) => r.uploaded)).toEqual([true, false]);
    expect(component.getMissingCount(1)).toBe(1);
    expect(component.getDocRequirements(2)).toEqual([]);
  });

  it('list_backendFails_showsAnEmptyList', () => {
    TestBed.configureTestingModule({
      imports: [ApplicationsComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        {
          provide: ApplicationService,
          useValue: { getMyApplications: () => throwError(() => new Error('boom')) },
        },
        { provide: AuthService, useValue: { currentUserValue: null } },
        { provide: DocumentService, useValue: {} },
        { provide: DocumentTypeService, useValue: {} },
      ],
    });
    const fixture = TestBed.createComponent(ApplicationsComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.applications).toEqual([]);
    expect(fixture.componentInstance.isLoading).toBeFalse();
  });
});
