import { TestBed, ComponentFixture } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { AddExistingAdminDialogComponent } from './add-existing-admin-dialog.component';
import { TenantService, UnassignedAdmin } from '../../../../../core/services/tenant.service';

/**
 * #413: a company's admin screen adds an existing account instead of creating one. The accounts
 * on offer are tenant admins made in user management with no company yet.
 */
describe('AddExistingAdminDialogComponent', () => {
  let fixture: ComponentFixture<AddExistingAdminDialogComponent>;
  let component: AddExistingAdminDialogComponent;
  let tenantService: jasmine.SpyObj<TenantService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AddExistingAdminDialogComponent>>;

  const thandi: UnassignedAdmin = {
    id: 10,
    email: 'thandi@acme.test',
    firstName: 'Thandi',
    lastName: 'Mokoena',
    userRole: 'TENANT_ADMIN',
    tenantId: 0,
    createdAt: '2026-11-04T09:00:00',
  };
  const sipho: UnassignedAdmin = {
    ...thandi,
    id: 11,
    email: 'sipho@acme.test',
    firstName: 'Sipho',
    lastName: 'Khumalo',
  };
  const staff: UnassignedAdmin = {
    ...thandi,
    id: 12,
    email: 'staff@acme.test',
    userRole: 'BUILDING_STAFF',
  };

  function create(waiting: UnassignedAdmin[] = [thandi, sipho, staff]): void {
    tenantService = jasmine.createSpyObj<TenantService>('TenantService', [
      'listUnassignedAdmins',
      'assignAdmin',
    ]);
    tenantService.listUnassignedAdmins.and.returnValue(of(waiting));
    dialogRef = jasmine.createSpyObj<MatDialogRef<AddExistingAdminDialogComponent>>(
      'MatDialogRef',
      ['close']
    );

    TestBed.configureTestingModule({
      imports: [AddExistingAdminDialogComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: TenantService, useValue: tenantService },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { tenantId: 5, tenantName: 'Acme Residences' } },
      ],
    });
    fixture = TestBed.createComponent(AddExistingAdminDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('offersOnlyTenantAdmins_becauseBuildingStaffHaveNoPlaceOnThisScreen', () => {
    create();

    expect(component.waiting.map((a) => a.id)).toEqual([10, 11]);
  });

  it('filtersByPartOfANameOrAnEmail_ignoringCase', () => {
    create();

    expect(component.matching('MOKO').map((a) => a.id)).toEqual([10]);
    expect(component.matching('sipho@').map((a) => a.id)).toEqual([11]);
    expect(component.matching('').length).toBe(2);
  });

  it('saysWhatWillHappen_beforeAnythingIsChanged', () => {
    create();

    component.choose(thandi);
    fixture.detectChanges();

    expect(text()).toContain(
      'Thandi Mokoena becomes an admin of Acme Residences and can sign in now.'
    );
    expect(text()).toContain('They will be asked to change their password.');
    expect(tenantService.assignAdmin).not.toHaveBeenCalled();
  });

  it('addsTheChosenAdminToThisCompany_andHandsThemBackForTheSuccessMessage', () => {
    create();
    tenantService.assignAdmin.and.returnValue(of({ ...thandi, tenantId: 5 }));

    component.choose(thandi);
    component.confirm();

    expect(tenantService.assignAdmin).toHaveBeenCalledWith(10, 5);
    expect(dialogRef.close).toHaveBeenCalledWith(thandi);
  });

  it('cannotConfirm_untilSomeoneIsChosen', () => {
    create();

    component.confirm();

    expect(tenantService.assignAdmin).not.toHaveBeenCalled();
  });

  it('explainsALostRace_andRefreshesTheList', () => {
    create();
    tenantService.assignAdmin.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { message: 'User 10 already belongs to a property company' },
          })
      )
    );
    tenantService.listUnassignedAdmins.and.returnValue(of([sipho]));

    component.choose(thandi);
    component.confirm();
    fixture.detectChanges();

    expect(text()).toContain('Thandi Mokoena was just added to another company.');
    expect(component.chosen).toBeNull();
    expect(component.waiting.map((a) => a.id)).toEqual([11]);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('showsTheServersReason_forAnyOtherRefusal', () => {
    create();
    tenantService.assignAdmin.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { message: "Property company 'Acme Residences' is deactivated" },
          })
      )
    );

    component.choose(thandi);
    component.confirm();
    fixture.detectChanges();

    expect(text()).toContain("Property company 'Acme Residences' is deactivated");
    expect(component.isSaving).toBeFalse();
  });

  it('pointsToUserManagement_whenNobodyIsWaiting', () => {
    create([staff]);

    expect(text()).toContain('No admins are waiting for a company.');
    const link = (fixture.nativeElement as HTMLElement).querySelector('a[href="/admin/users"]');
    expect(link).withContext('a link to user management').not.toBeNull();
  });

  it('saysSo_whenTheListCannotLoad', () => {
    create();
    tenantService.listUnassignedAdmins.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500 }))
    );

    component.load();
    fixture.detectChanges();

    expect(text()).toContain('The list of admins waiting for a company could not be loaded.');
  });
});
