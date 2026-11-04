import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { AllocationChoiceDialogComponent } from './allocation-choice-dialog.component';
import { ApplicationService } from '../../../../core/services/application.service';
import { Application } from '../../../../core/models/application.model';
import { BuildingService } from '../../../../core/services/building.service';
import { EligibleRoomResponse, RoomService } from '../../../../core/services/room.service';
import { StagedAllocationService } from '../../../../core/services/staged-allocation.service';

function room(overrides: Partial<EligibleRoomResponse> = {}): EligibleRoomResponse {
  return { roomId: 5, roomNumber: 'A101', ...overrides } as EligibleRoomResponse;
}

describe('AllocationChoiceDialogComponent', () => {
  let fixture: ComponentFixture<AllocationChoiceDialogComponent>;
  let component: AllocationChoiceDialogComponent;
  let rooms: jasmine.SpyObj<RoomService>;
  let staged: jasmine.SpyObj<StagedAllocationService>;
  let applications: jasmine.SpyObj<ApplicationService>;
  let close: jasmine.Spy;

  function create(data: { applicationId: number; studentId?: number }): void {
    rooms = jasmine.createSpyObj<RoomService>('RoomService', ['getEligibleRoomsForStudent']);
    rooms.getEligibleRoomsForStudent.and.returnValue(
      of([room(), room({ roomId: 6, roomNumber: 'B202' })])
    );
    staged = jasmine.createSpyObj<StagedAllocationService>('StagedAllocationService', [
      'enqueueWaitingList',
      'overrideConstraints',
      'confirmAllocation',
    ]);
    applications = jasmine.createSpyObj<ApplicationService>('ApplicationService', [
      'getApplicationById',
    ]);
    close = jasmine.createSpy('close');

    TestBed.configureTestingModule({
      imports: [AllocationChoiceDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: RoomService, useValue: rooms },
        { provide: StagedAllocationService, useValue: staged },
        { provide: ApplicationService, useValue: applications },
        { provide: BuildingService, useValue: { getAllBuildingsNoPagination: () => of([]) } },
        { provide: MatDialogRef, useValue: { close } },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    });
    fixture = TestBed.createComponent(AllocationChoiceDialogComponent);
    component = fixture.componentInstance;
  }

  it('ngOnInit_studentKnown_loadsEligibleRooms', () => {
    create({ applicationId: 7, studentId: 3 });

    fixture.detectChanges();

    expect(rooms.getEligibleRoomsForStudent).toHaveBeenCalledOnceWith(3, undefined);
    expect(component.eligibleRooms.length).toBe(2);
    expect(component.isLoading).toBeFalse();
  });

  it('ngOnInit_studentMissing_readsItFromTheApplication', () => {
    create({ applicationId: 7 });
    applications.getApplicationById.and.returnValue(of({ studentId: 3 } as Application));

    fixture.detectChanges();

    expect(component.studentId).toBe(3);
    expect(rooms.getEligibleRoomsForStudent).toHaveBeenCalled();
  });

  it('ngOnInit_applicationHasNoStudent_showsAnError', () => {
    create({ applicationId: 7 });
    applications.getApplicationById.and.returnValue(of({} as Application));

    fixture.detectChanges();

    expect(component.error).toContain('student ID');
  });

  it('ngOnInit_applicationFailsToLoad_showsAnError', () => {
    create({ applicationId: 7 });
    applications.getApplicationById.and.returnValue(throwError(() => new Error('boom')));

    fixture.detectChanges();

    expect(component.error).toContain('application details');
    expect(component.isLoading).toBeFalse();
  });

  it('onSearch_roomNumberEntered_filtersTheRooms', () => {
    create({ applicationId: 7, studentId: 3 });
    fixture.detectChanges();

    component.searchForm.patchValue({ roomNumber: 'b2' });
    component.onSearch();

    expect(component.eligibleRooms.map((r) => r.roomNumber)).toEqual(['B202']);
  });

  it('loadEligibleRooms_backendFails_stopsLoading', () => {
    create({ applicationId: 7, studentId: 3 });
    fixture.detectChanges();
    rooms.getEligibleRoomsForStudent.and.returnValue(throwError(() => new Error('boom')));

    component.loadEligibleRooms();

    expect(component.isLoading).toBeFalse();
  });

  it('addToWaitingList_success_closesAsWaitlisted', () => {
    create({ applicationId: 7, studentId: 3 });
    fixture.detectChanges();
    staged.enqueueWaitingList.and.returnValue(of({ message: 'Added' }) as never);

    component.addToWaitingList();

    expect(staged.enqueueWaitingList).toHaveBeenCalledOnceWith(3, 7);
    expect(close).toHaveBeenCalledOnceWith({ action: 'waitlisted' });
  });

  it('addToWaitingList_backendFails_staysOpen', () => {
    create({ applicationId: 7, studentId: 3 });
    fixture.detectChanges();
    staged.enqueueWaitingList.and.returnValue(throwError(() => new Error('boom')));

    component.addToWaitingList();

    expect(close).not.toHaveBeenCalled();
    expect(component.isAddingToWaitingList).toBeFalse();
  });

  it('addToWaitingList_studentUnknown_callsNothing', () => {
    create({ applicationId: 7 });
    component.studentId = null;

    component.addToWaitingList();

    expect(staged.enqueueWaitingList).not.toHaveBeenCalled();
  });

  it('selectRoom_success_confirmsAndCloses', () => {
    create({ applicationId: 7, studentId: 3 });
    fixture.detectChanges();
    staged.overrideConstraints.and.returnValue(of({ id: 9 }) as never);
    staged.confirmAllocation.and.returnValue(of({}) as never);

    component.selectRoom(room());

    expect(staged.overrideConstraints).toHaveBeenCalledOnceWith(3, 5, 'Manual Selection', []);
    expect(staged.confirmAllocation).toHaveBeenCalledOnceWith(9);
    expect(close).toHaveBeenCalledOnceWith({ action: 'allocated' });
  });

  it('selectRoom_confirmFails_staysOpen', () => {
    create({ applicationId: 7, studentId: 3 });
    fixture.detectChanges();
    staged.overrideConstraints.and.returnValue(of({ id: 9 }) as never);
    staged.confirmAllocation.and.returnValue(throwError(() => new Error('boom')));

    component.selectRoom(room());

    expect(close).not.toHaveBeenCalled();
    expect(component.isAllocating).toBeFalse();
  });

  it('selectRoom_overrideFails_staysOpen', () => {
    create({ applicationId: 7, studentId: 3 });
    fixture.detectChanges();
    staged.overrideConstraints.and.returnValue(throwError(() => new Error('boom')));

    component.selectRoom(room());

    expect(staged.confirmAllocation).not.toHaveBeenCalled();
    expect(component.isAllocating).toBeFalse();
  });

  it('selectRoom_studentUnknown_callsNothing', () => {
    create({ applicationId: 7 });
    component.studentId = null;

    component.selectRoom(room());

    expect(staged.overrideConstraints).not.toHaveBeenCalled();
  });

  it('getYearRestrictionLabel_coversEachRestrictionShape', () => {
    create({ applicationId: 7, studentId: 3 });

    expect(component.getYearRestrictionLabel(room())).toBe('');
    expect(component.getYearRestrictionLabel(room({ minYearOfStudy: 2, maxYearOfStudy: 3 }))).toBe(
      'Year 2-3'
    );
    expect(component.getYearRestrictionLabel(room({ minYearOfStudy: 2 }))).toBe('Year 2+');
    expect(component.getYearRestrictionLabel(room({ maxYearOfStudy: 3 }))).toBe('Year 1-3');
  });
});
