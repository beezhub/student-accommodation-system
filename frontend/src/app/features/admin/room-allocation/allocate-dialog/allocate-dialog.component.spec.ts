import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { AllocateDialogComponent } from './allocate-dialog.component';
import {
  AllocationEligibilityResponse,
  RoomAllocationResponse,
  RoomAllocationService,
  RoomAvailabilityResponse,
} from '../../../../core/services/room-allocation.service';
import { RoomResponse, RoomService } from '../../../../core/services/room.service';
import { AdminService, ApplicationResponse } from '../../../../core/services/admin.service';

describe('AllocateDialogComponent', () => {
  const eligible: AllocationEligibilityResponse = {
    studentId: 7,
    isEligible: true,
    hasApprovedApplication: true,
    hasActiveAllocation: false,
  };
  const available: RoomAvailabilityResponse = {
    roomId: 3,
    roomNumber: 'A101',
    buildingName: 'North Hall',
    capacity: 2,
    currentOccupancy: 0,
    availableSpots: 2,
    isAvailable: true,
    status: 'AVAILABLE',
  };
  const room = { id: 3, roomNumber: 'A101', buildingName: 'North Hall', status: 'AVAILABLE' };
  const application = { id: 11, applicationCode: 'APP-2027-11', status: 'APPROVED' };

  let allocationService: jasmine.SpyObj<RoomAllocationService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let dialogRef: { close: jasmine.Spy };
  let fixture: ComponentFixture<AllocateDialogComponent>;
  let component: AllocateDialogComponent;

  beforeEach(() => {
    allocationService = jasmine.createSpyObj('RoomAllocationService', {
      checkStudentEligibility: of(eligible),
      getRoomAvailability: of(available),
      allocateByApplication: of({ id: 1 } as RoomAllocationResponse),
      createAllocation: of({ id: 1 } as RoomAllocationResponse),
    });
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialogRef = { close: jasmine.createSpy('close') };
    spyOn(console, 'error');
    TestBed.configureTestingModule({
      imports: [AllocateDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: RoomAllocationService, useValue: allocationService },
        {
          provide: RoomService,
          useValue: {
            searchRooms: () => of({ content: [room as RoomResponse], totalElements: 1 }),
          },
        },
        {
          provide: AdminService,
          useValue: {
            searchApplications: () =>
              of({ content: [application as ApplicationResponse], totalElements: 1 }),
          },
        },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: {} },
      ],
    });
    TestBed.overrideProvider(MatSnackBar, { useValue: snackBar });
    fixture = TestBed.createComponent(AllocateDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  /** Fills the form and runs both checks, as the admin does by leaving the field and picking. */
  function ready(applicationId: number | '' = ''): void {
    component.allocateForm.patchValue({ studentId: 7, roomId: 3, applicationId });
    component.checkEligibility();
    fixture.debugElement
      .query(By.css('app-allocation-room-field'))
      .triggerEventHandler('roomSelected', undefined);
    fixture.detectChanges();
  }

  it('render_newDialog_showsTheFieldsAndBlocksAllocate', () => {
    expect(fixture.debugElement.query(By.css('app-allocation-room-field'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('app-allocation-application-field'))).toBeTruthy();
    expect(text()).toContain('Allocate Room to Student');
    expect(text()).not.toContain('Allocation Process');
    expect(component.canSubmit()).toBeFalse();
    const submit = fixture.debugElement.query(By.css('.submit-btn')).nativeElement;
    expect((submit as HTMLButtonElement).disabled).toBeTrue();
  });

  it('checkEligibility_eligibleStudent_showsTheGreenMessage', () => {
    component.allocateForm.patchValue({ studentId: 7 });
    fixture.debugElement
      .query(By.css('input[formControlName="studentId"]'))
      .triggerEventHandler('blur', {});
    fixture.detectChanges();

    expect(allocationService.checkStudentEligibility).toHaveBeenCalledWith(7);
    expect(component.eligibilityChecked).toBeTrue();
    expect(fixture.debugElement.query(By.css('.status-message.success'))).toBeTruthy();
    expect(text()).toContain('Student is eligible for room allocation');
  });

  it('checkEligibility_notEligible_showsTheReason', () => {
    allocationService.checkStudentEligibility.and.returnValue(
      of({ ...eligible, isEligible: false, reason: 'Student already has a room' })
    );
    component.allocateForm.patchValue({ studentId: 7 });
    component.checkEligibility();
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.status-message.error'))).toBeTruthy();
    expect(text()).toContain('Student already has a room');
  });

  it('checkEligibility_failedLookup_treatsTheStudentAsNotEligible', () => {
    allocationService.checkStudentEligibility.and.returnValue(throwError(() => new Error('down')));
    component.allocateForm.patchValue({ studentId: 7 });
    component.checkEligibility();
    fixture.detectChanges();

    expect(component.eligibility?.isEligible).toBeFalse();
    expect(text()).toContain('Failed to check eligibility');
  });

  it('checkEligibility_noStudentId_clearsTheLastResult', () => {
    component.allocateForm.patchValue({ studentId: 7 });
    component.checkEligibility();
    component.allocateForm.patchValue({ studentId: '' });
    component.checkEligibility();

    expect(allocationService.checkStudentEligibility).toHaveBeenCalledTimes(1);
    expect(component.eligibilityChecked).toBeFalse();
    expect(component.eligibility).toBeNull();
  });

  it('checkAvailability_roomPicked_showsTheSpotsLeft', () => {
    ready();

    expect(allocationService.getRoomAvailability).toHaveBeenCalledWith(3);
    expect(text()).toContain('Room is available (2 spots remaining)');
  });

  it('checkAvailability_failedLookup_marksTheRoomUnavailable', () => {
    allocationService.getRoomAvailability.and.returnValue(throwError(() => new Error('down')));
    ready();

    expect(component.availability?.isAvailable).toBeFalse();
    expect(text()).toContain('Room is not available (ERROR)');
    expect(component.canSubmit()).toBeFalse();
  });

  it('checkAvailability_noRoom_clearsTheLastResult', () => {
    ready();
    component.allocateForm.patchValue({ roomId: '' });
    component.checkAvailability();

    expect(component.availabilityChecked).toBeFalse();
    expect(component.availability).toBeNull();
  });

  it('canSubmit_bothChecksPass_enablesAllocate', () => {
    ready();

    expect(component.canSubmit()).toBeTrue();
    const submit = fixture.debugElement.query(By.css('.submit-btn')).nativeElement;
    expect((submit as HTMLButtonElement).disabled).toBeFalse();
  });

  it('onSubmit_applicationChosen_allocatesThroughTheApplication', () => {
    ready(11);
    expect(text()).toContain('Application status will be updated');

    component.onSubmit();

    expect(allocationService.allocateByApplication).toHaveBeenCalledWith({
      applicationId: 11,
      roomId: 3,
    });
    expect(allocationService.createAllocation).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith('Room allocated successfully', 'Close', {
      duration: 3000,
    });
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('onSubmit_noApplication_allocatesByStudentAndDate', () => {
    const date = new Date(2027, 1, 1);
    ready();
    component.allocateForm.patchValue({ allocationDate: date });

    component.onSubmit();

    expect(allocationService.createAllocation).toHaveBeenCalledWith({
      studentId: 7,
      roomId: 3,
      allocationDate: date.toISOString(),
    });
    expect(allocationService.allocateByApplication).not.toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('onSubmit_serverRefuses_showsItsMessageAndStaysOpen', () => {
    allocationService.createAllocation.and.returnValue(
      throwError(() => ({ error: { message: 'Room is full' } }))
    );
    ready();

    component.onSubmit();

    expect(snackBar.open).toHaveBeenCalledWith('Room is full', 'Close', { duration: 5000 });
    expect(component.isSubmitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('onSubmit_checksNotRun_sendsNothing', () => {
    component.allocateForm.patchValue({ studentId: 7, roomId: 3 });

    component.onSubmit();

    expect(allocationService.createAllocation).not.toHaveBeenCalled();
    expect(allocationService.allocateByApplication).not.toHaveBeenCalled();
    expect(component.isSubmitting).toBeFalse();
  });

  it('onCancel_closesWithoutAllocating', () => {
    component.onCancel();

    expect(dialogRef.close).toHaveBeenCalledWith(false);
  });
});
