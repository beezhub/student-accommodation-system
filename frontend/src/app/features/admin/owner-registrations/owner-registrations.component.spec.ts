import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { OwnerRegistrationsComponent } from './owner-registrations.component';
import {
  OwnerRegistrationAdminService,
  OwnerRegistrationSummary,
} from './owner-registration-admin.service';
import { OwnerRegistrationReviewDialogComponent } from './owner-registration-review-dialog/owner-registration-review-dialog.component';

const mockRegistrations: OwnerRegistrationSummary[] = [
  {
    id: 1,
    fullName: 'Jane Smith',
    email: 'jane@acme.com',
    organisationName: 'Acme Properties',
    contactPhone: '0712345678',
    message: null,
    status: 'PENDING_REVIEW',
    createdAt: '2026-01-01T10:00:00Z',
    verifiedAt: '2026-01-01T11:00:00Z',
  },
  {
    id: 2,
    fullName: 'Bob Jones',
    email: 'bob@example.com',
    organisationName: 'Example Ltd',
    contactPhone: null,
    message: 'Looking forward to it',
    status: 'PENDING',
    createdAt: '2026-01-02T09:00:00Z',
    verifiedAt: null,
  },
];

describe('OwnerRegistrationsComponent', () => {
  let component: OwnerRegistrationsComponent;
  let serviceSpy: jasmine.SpyObj<OwnerRegistrationAdminService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    serviceSpy = jasmine.createSpyObj<OwnerRegistrationAdminService>(
      'OwnerRegistrationAdminService',
      ['getPendingRegistrations']
    );
    dialogSpy = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snackBarSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [OwnerRegistrationsComponent, NoopAnimationsModule],
      providers: [
        { provide: OwnerRegistrationAdminService, useValue: serviceSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    }).compileComponents();
  });

  function createComponent(): void {
    const fixture = TestBed.createComponent(OwnerRegistrationsComponent);
    component = fixture.componentInstance;
  }

  it('starts with isLoading = true before ngOnInit completes', () => {
    serviceSpy.getPendingRegistrations.and.returnValue(of([]));
    createComponent();

    expect(component.isLoading).toBeTrue();
  });

  it('populates registrations and clears loading on successful fetch', fakeAsync(() => {
    serviceSpy.getPendingRegistrations.and.returnValue(of(mockRegistrations));
    createComponent();

    component.ngOnInit();
    tick();

    expect(component.registrations).toEqual(mockRegistrations);
    expect(component.isLoading).toBeFalse();
  }));

  it('returns empty pagedRegistrations when registrations is empty', fakeAsync(() => {
    serviceSpy.getPendingRegistrations.and.returnValue(of([]));
    createComponent();

    component.ngOnInit();
    tick();

    expect(component.pagedRegistrations).toEqual([]);
  }));

  it('pagedRegistrations returns first page slice', fakeAsync(() => {
    serviceSpy.getPendingRegistrations.and.returnValue(of(mockRegistrations));
    createComponent();
    component.pageSize = 1;
    component.pageIndex = 0;

    component.ngOnInit();
    tick();

    expect(component.pagedRegistrations.length).toBe(1);
    expect(component.pagedRegistrations[0].id).toBe(1);
  }));

  it('pagedRegistrations returns second page slice', fakeAsync(() => {
    serviceSpy.getPendingRegistrations.and.returnValue(of(mockRegistrations));
    createComponent();
    component.pageSize = 1;
    component.pageIndex = 1;

    component.ngOnInit();
    tick();

    expect(component.pagedRegistrations.length).toBe(1);
    expect(component.pagedRegistrations[0].id).toBe(2);
  }));

  it('shows error snackbar and clears loading when fetch fails', fakeAsync(() => {
    serviceSpy.getPendingRegistrations.and.returnValue(throwError(() => new Error('Network error')));
    createComponent();

    component.ngOnInit();
    tick();

    expect(component.isLoading).toBeFalse();
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Failed to load registrations.',
      'Close',
      jasmine.objectContaining({ panelClass: ['error-snackbar'] })
    );
  }));

  it('openReview opens dialog with registration data', fakeAsync(() => {
    serviceSpy.getPendingRegistrations.and.returnValue(of(mockRegistrations));
    const dialogRefSpy = jasmine.createSpyObj<MatDialogRef<OwnerRegistrationReviewDialogComponent>>(
      'MatDialogRef',
      { afterClosed: of(false) }
    );
    dialogSpy.open.and.returnValue(dialogRefSpy as any);
    createComponent();

    component.ngOnInit();
    tick();

    component.openReview(mockRegistrations[0]);

    expect(dialogSpy.open).toHaveBeenCalledWith(
      OwnerRegistrationReviewDialogComponent,
      jasmine.objectContaining({ data: mockRegistrations[0] })
    );
  }));

  it('reloads registrations after dialog closes with refreshNeeded=true', fakeAsync(() => {
    serviceSpy.getPendingRegistrations.and.returnValue(of(mockRegistrations));
    const dialogRefSpy = jasmine.createSpyObj<MatDialogRef<OwnerRegistrationReviewDialogComponent>>(
      'MatDialogRef',
      { afterClosed: of(true) }
    );
    dialogSpy.open.and.returnValue(dialogRefSpy as any);
    createComponent();

    component.ngOnInit();
    tick();

    component.openReview(mockRegistrations[0]);
    tick();

    expect(serviceSpy.getPendingRegistrations).toHaveBeenCalledTimes(2);
  }));

  it('does not reload after dialog closes with refreshNeeded=false', fakeAsync(() => {
    serviceSpy.getPendingRegistrations.and.returnValue(of(mockRegistrations));
    const dialogRefSpy = jasmine.createSpyObj<MatDialogRef<OwnerRegistrationReviewDialogComponent>>(
      'MatDialogRef',
      { afterClosed: of(false) }
    );
    dialogSpy.open.and.returnValue(dialogRefSpy as any);
    createComponent();

    component.ngOnInit();
    tick();

    component.openReview(mockRegistrations[0]);
    tick();

    expect(serviceSpy.getPendingRegistrations).toHaveBeenCalledTimes(1);
  }));

  it('onPageChange updates pageIndex and pageSize', () => {
    serviceSpy.getPendingRegistrations.and.returnValue(of([]));
    createComponent();

    component.onPageChange({ pageIndex: 2, pageSize: 25, length: 100 } as any);

    expect(component.pageIndex).toBe(2);
    expect(component.pageSize).toBe(25);
  });

  it('getStatusLabel returns human-readable labels', () => {
    serviceSpy.getPendingRegistrations.and.returnValue(of([]));
    createComponent();

    expect(component.getStatusLabel('PENDING')).toBe('Pending Email');
    expect(component.getStatusLabel('PENDING_REVIEW')).toBe('Pending Review');
    expect(component.getStatusLabel('APPROVED')).toBe('Approved');
    expect(component.getStatusLabel('REJECTED')).toBe('Rejected');
    expect(component.getStatusLabel('UNKNOWN')).toBe('UNKNOWN');
  });

  it('getStatusClass returns correct CSS class', () => {
    serviceSpy.getPendingRegistrations.and.returnValue(of([]));
    createComponent();

    expect(component.getStatusClass('PENDING')).toBe('status-pending');
    expect(component.getStatusClass('PENDING_REVIEW')).toBe('status-review');
    expect(component.getStatusClass('APPROVED')).toBe('status-approved');
    expect(component.getStatusClass('REJECTED')).toBe('status-rejected');
    expect(component.getStatusClass('OTHER')).toBe('');
  });
});
