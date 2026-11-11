import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ReconfirmPlaceComponent } from './reconfirm-place.component';
import {
  RoomAllocationResponse,
  RoomAllocationService,
} from '../../../core/services/room-allocation.service';

describe('ReconfirmPlaceComponent', () => {
  let fixture: ComponentFixture<ReconfirmPlaceComponent>;
  let component: ReconfirmPlaceComponent;
  let service: jasmine.SpyObj<RoomAllocationService>;

  const base: RoomAllocationResponse = {
    id: 7,
    studentId: 1,
    studentFirstName: 'Ada',
    studentLastName: 'Test',
    studentEmail: 'ada@test.local',
    roomId: 3,
    roomNumber: '12',
    buildingName: 'West',
    allocationDate: '2026-06-01T10:00:00',
    status: 'ACCEPTED',
    stayStartDate: '2027-02-01',
    createdAt: '2026-06-01T10:00:00',
    updatedAt: '2026-06-01T10:00:00',
  };

  function build(allocation: RoomAllocationResponse): void {
    service = jasmine.createSpyObj<RoomAllocationService>('RoomAllocationService', ['reconfirm']);
    TestBed.configureTestingModule({
      imports: [ReconfirmPlaceComponent],
      providers: [{ provide: RoomAllocationService, useValue: service }],
    });
    fixture = TestBed.createComponent(ReconfirmPlaceComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('allocation', allocation);
    fixture.detectChanges();
  }

  it('asked and not answered: shows "Still coming?" with the move-in day', () => {
    build({ ...base, reconfirmRequestedAt: '2027-01-04T08:00:00' });

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Still coming?');
    expect(text).toContain('1 February 2027');
  });

  it('not asked: shows nothing', () => {
    build(base);

    expect((fixture.nativeElement as HTMLElement).textContent?.trim()).toBe('');
  });

  it('confirm: calls the service and emits the updated allocation', () => {
    build({ ...base, reconfirmRequestedAt: '2027-01-04T08:00:00' });
    const updated = { ...base, reconfirmedAt: '2027-01-05T09:00:00' };
    service.reconfirm.and.returnValue(of(updated));
    const emitted: RoomAllocationResponse[] = [];
    component.reconfirmed.subscribe((a) => emitted.push(a));

    component.confirm();

    expect(service.reconfirm).toHaveBeenCalledWith(7);
    expect(emitted).toEqual([updated]);
    expect(component.processing).toBeFalse();
  });

  it('confirm fails: stays on the question and re-enables the button', () => {
    build({ ...base, reconfirmRequestedAt: '2027-01-04T08:00:00' });
    service.reconfirm.and.returnValue(throwError(() => new Error('down')));

    component.confirm();

    expect(component.processing).toBeFalse();
  });

  it('confirmed: says so', () => {
    build({
      ...base,
      reconfirmRequestedAt: '2027-01-04T08:00:00',
      reconfirmedAt: '2027-01-05T09:00:00',
    });

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Place confirmed');
  });
});
