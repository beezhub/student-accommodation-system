import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatSelect } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { AllocationRoomFieldComponent } from './allocation-room-field.component';
import { RoomAvailabilityResponse } from '../../../../../core/services/room-allocation.service';
import { RoomResponse, RoomService } from '../../../../../core/services/room.service';

describe('AllocationRoomFieldComponent', () => {
  const room = (id: number, status: string): RoomResponse =>
    ({ id, roomNumber: `A${id}`, buildingName: 'North Hall', status } as RoomResponse);
  const availability: RoomAvailabilityResponse = {
    roomId: 3,
    roomNumber: 'A3',
    buildingName: 'North Hall',
    capacity: 2,
    currentOccupancy: 0,
    availableSpots: 2,
    isAvailable: true,
    status: 'AVAILABLE',
  };

  let searchRooms: jasmine.Spy;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let fixture: ComponentFixture<AllocationRoomFieldComponent>;
  let component: AllocationRoomFieldComponent;
  let group: FormGroup;

  beforeEach(() => {
    searchRooms = jasmine.createSpy('searchRooms').and.returnValue(
      of({
        content: [room(1, 'AVAILABLE'), room(2, 'MAINTENANCE'), room(3, 'OCCUPIED')],
        totalElements: 40,
      })
    );
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    spyOn(console, 'error');
    TestBed.configureTestingModule({
      imports: [AllocationRoomFieldComponent],
      providers: [provideNoopAnimations(), { provide: RoomService, useValue: { searchRooms } }],
    });
    TestBed.overrideProvider(MatSnackBar, { useValue: snackBar });
  });

  function build(checked: RoomAvailabilityResponse | null = null): void {
    group = new FormGroup({ roomId: new FormControl<number | ''>('') });
    fixture = TestBed.createComponent(AllocationRoomFieldComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('group', group);
    fixture.componentRef.setInput('availability', checked);
    fixture.detectChanges();
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('ngOnInit_firstPage_listsTheRoomsThatCanTakeAStudent', () => {
    build();

    expect(searchRooms).toHaveBeenCalledWith({ page: 0, size: 25, roomNumber: undefined });
    expect(component.availableRooms.map((r) => r.id)).toEqual([1, 3]);
    expect(component.moreRoomsExist).toBeTrue();
  });

  it('roomSearch_typedNumber_searchesTheServerAfterAPause', fakeAsync(() => {
    build();
    searchRooms.calls.reset();

    component.roomSearch.setValue('A10');
    tick(299);
    expect(searchRooms).not.toHaveBeenCalled();
    tick(1);

    expect(searchRooms).toHaveBeenCalledWith({ page: 0, size: 25, roomNumber: 'A10' });
  }));

  it('loadAvailableRooms_serverError_tellsTheAdmin', () => {
    searchRooms.and.returnValue(throwError(() => new Error('down')));
    build();

    expect(component.availableRooms).toEqual([]);
    expect(snackBar.open).toHaveBeenCalledWith('Failed to load available rooms', 'Close', {
      duration: 3000,
    });
  });

  it('group_roomIdControl_isTheSelectValue', () => {
    build();
    group.get('roomId')!.setValue(3);
    fixture.detectChanges();

    const select = fixture.debugElement.query(By.directive(MatSelect)).componentInstance;
    expect((select as MatSelect).value).toBe(3);
  });

  it('availability_notChecked_showsNoStatus', () => {
    build();

    expect(fixture.debugElement.query(By.css('.status-message'))).toBeNull();
  });

  it('availability_available_showsTheSpotsLeft', () => {
    build(availability);

    expect(fixture.debugElement.query(By.css('.status-message.success'))).toBeTruthy();
    expect(text()).toContain('Room is available (2 spots remaining)');
  });

  it('availability_unavailable_showsTheRoomStatus', () => {
    build({ ...availability, isAvailable: false, availableSpots: 0, status: 'FULL' });

    expect(fixture.debugElement.query(By.css('.status-message.error'))).toBeTruthy();
    expect(text()).toContain('Room is not available (FULL)');
  });

  it('roomSelected_selectionChange_emits', () => {
    build();
    const selected = jasmine.createSpy('roomSelected');
    component.roomSelected.subscribe(selected);

    fixture.debugElement.query(By.directive(MatSelect)).triggerEventHandler('selectionChange', {});

    expect(selected).toHaveBeenCalledTimes(1);
  });
});
