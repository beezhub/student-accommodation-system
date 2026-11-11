import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { StayExtensionComponent } from './stay-extension.component';
import {
  RoomAllocationResponse,
  RoomAllocationService,
  StayExtensionResponse,
} from '../../../core/services/room-allocation.service';

describe('StayExtensionComponent', () => {
  let fixture: ComponentFixture<StayExtensionComponent>;
  let component: StayExtensionComponent;
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
    allocationDate: '2027-01-10T10:00:00',
    status: 'ACTIVE',
    stayStartDate: '2027-02-01',
    stayEndDate: '2027-06-30',
    createdAt: '2027-01-10T10:00:00',
    updatedAt: '2027-01-10T10:00:00',
  };

  const allowed: StayExtensionResponse = {
    allocationId: 7,
    allowed: true,
    currentStayEndDate: '2027-06-30',
    nextIntakeId: 2,
    nextIntakeName: 'Semester 2',
    nextStayStartDate: '2027-07-15',
    nextStayEndDate: '2027-11-30',
  };

  function build(allocation: RoomAllocationResponse, option: StayExtensionResponse): void {
    service = jasmine.createSpyObj<RoomAllocationService>('RoomAllocationService', [
      'getStayExtension',
      'extendStay',
    ]);
    service.getStayExtension.and.returnValue(of(option));
    TestBed.configureTestingModule({
      imports: [StayExtensionComponent],
      providers: [provideRouter([]), { provide: RoomAllocationService, useValue: service }],
    });
    fixture = TestBed.createComponent(StayExtensionComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('allocation', allocation);
    fixture.detectChanges();
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent?.replace(/\s+/g, ' ').trim() ?? '';
  }

  it('bed free and documents in order: offers to stay for the next intake', () => {
    build(base, allowed);

    expect(service.getStayExtension).toHaveBeenCalledWith(7);
    expect(text()).toContain('Stay for Semester 2');
    expect(text()).not.toContain('Apply for');
  });

  it('not allowed: links to applying for the next intake instead', () => {
    build(base, { ...allowed, allowed: false, reason: 'This room is not free for Semester 2.' });

    expect(text()).toContain('Apply for Semester 2');
    expect(text()).not.toContain('Stay for');
    const link = (fixture.nativeElement as HTMLElement).querySelector('a');
    expect(link?.getAttribute('href')).toBe('/applications/new');
  });

  it('annual stay: shows nothing', () => {
    build(base, { allocationId: 7, allowed: false, reason: 'An Annual stay cannot be extended.' });

    expect(text()).toBe('');
  });

  it('an offer not yet accepted: does not ask', () => {
    build({ ...base, status: 'OFFERED' }, allowed);

    expect(service.getStayExtension).not.toHaveBeenCalled();
    expect(text()).toBe('');
  });

  it('stay: extends, confirms and emits the updated allocation', () => {
    build(base, allowed);
    const updated = { ...base, stayEndDate: '2027-11-30' };
    service.extendStay.and.returnValue(of(updated));
    const emitted: RoomAllocationResponse[] = [];
    component.extended.subscribe((a) => emitted.push(a));

    component.stay();
    fixture.detectChanges();

    expect(service.extendStay).toHaveBeenCalledWith(7);
    expect(emitted).toEqual([updated]);
    expect(text()).toContain('You are staying for Semester 2');
    expect(text()).toContain('30 November 2027');
  });

  it('bed taken since the page loaded: shows the reason and the same apply link', () => {
    build(base, allowed);
    service.extendStay.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: {
              message: 'This room is not free for Semester 2. You can apply for it instead.',
            },
          })
      )
    );

    component.stay();
    fixture.detectChanges();

    expect(component.processing).toBeFalse();
    expect(text()).toContain('This room is not free for Semester 2.');
    expect(text()).toContain('Apply for Semester 2');
  });
});
