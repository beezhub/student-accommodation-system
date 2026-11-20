import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AssignAcademicYearDialogComponent } from './assign-academic-year-dialog.component';
import { AcademicYear } from '../../../../core/models/academic-year.model';
import { AcademicYearService } from '../../../../core/services/academic-year.service';
import { Tenant, TenantService } from '../../../../core/services/tenant.service';

describe('AssignAcademicYearDialogComponent', () => {
  const platformYear: AcademicYear = {
    id: 5,
    tenantId: 0,
    name: '2027',
    startDate: '2027-02-01',
    endDate: '2027-11-30',
    isApplicationWindowOpen: false,
  };
  const tenants: Tenant[] = [
    { id: 0, name: 'PLATFORM', isActive: true },
    { id: 42, name: 'Acme Residences', isActive: true },
  ];

  let academicYearService: jasmine.SpyObj<AcademicYearService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AssignAcademicYearDialogComponent>>;

  function create(): AssignAcademicYearDialogComponent {
    academicYearService = jasmine.createSpyObj<AcademicYearService>('AcademicYearService', [
      'assignToCompany',
    ]);
    const tenantService = jasmine.createSpyObj<TenantService>('TenantService', {
      getActiveTenants: of(tenants),
    });
    dialogRef = jasmine.createSpyObj<MatDialogRef<AssignAcademicYearDialogComponent>>(
      'MatDialogRef',
      ['close']
    );
    TestBed.configureTestingModule({
      imports: [AssignAcademicYearDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: AcademicYearService, useValue: academicYearService },
        { provide: TenantService, useValue: tenantService },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { academicYear: platformYear } },
      ],
    });
    const fixture = TestBed.createComponent(AssignAcademicYearDialogComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('ngOnInit_offersActiveCompaniesButNotThePlatform', () => {
    const component = create();

    expect(component.companies.map((c) => c.id)).toEqual([42]);
  });

  it('onSubmit_noCompanyChosen_doesNotCallTheServer', () => {
    const component = create();

    component.onSubmit();

    expect(academicYearService.assignToCompany).not.toHaveBeenCalled();
    expect(component.companyId.touched).toBeTrue();
  });

  it('onSubmit_assignsAndClosesWithTheCompanysCopy', () => {
    const component = create();
    const copy: AcademicYear = {
      ...platformYear,
      id: 9,
      tenantId: 42,
      companyName: 'Acme Residences',
    };
    academicYearService.assignToCompany.and.returnValue(of(copy));
    component.companyId.setValue(42);

    component.onSubmit();

    expect(academicYearService.assignToCompany).toHaveBeenCalledWith(5, 42);
    expect(dialogRef.close).toHaveBeenCalledWith(copy);
  });

  it('onSubmit_serverRefuses_showsItsMessageAndStaysOpen', () => {
    const component = create();
    academicYearService.assignToCompany.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { message: "There is already a year called '2027' for All institutions" },
          })
      )
    );
    component.companyId.setValue(42);

    component.onSubmit();

    expect(component.serverError).toBe(
      "There is already a year called '2027' for All institutions"
    );
    expect(component.isSubmitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
