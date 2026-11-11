import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { OverdueCheckOutsComponent } from './overdue-check-outs.component';
import { AdminService } from '../../../../core/services/admin.service';

describe('OverdueCheckOutsComponent', () => {
  function build(response: unknown, fails = false) {
    TestBed.configureTestingModule({
      imports: [OverdueCheckOutsComponent],
      providers: [
        provideRouter([]),
        {
          provide: AdminService,
          useValue: {
            getOverdueCheckOuts: () => (fails ? throwError(() => new Error('down')) : of(response)),
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(OverdueCheckOutsComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('lists students still checked in after their stay ended', () => {
    const fixture = build({
      content: [
        {
          id: 1,
          studentFirstName: 'Ada',
          studentLastName: 'Lovelace',
          roomNumber: '12',
          buildingName: 'West',
          stayEndDate: '2026-11-01',
        },
      ],
      totalElements: 1,
    });

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Overdue check-outs (1)');
    expect(text).toContain('Ada Lovelace');
    expect(text).toContain('Stay ended 1 Nov 2026');
  });

  it('shows nothing when none are overdue', () => {
    const fixture = build({ content: [], totalElements: 0 });

    expect((fixture.nativeElement as HTMLElement).textContent?.trim()).toBe('');
  });

  it('shows nothing when the lookup fails', () => {
    const fixture = build(null, true);

    expect(fixture.componentInstance.total).toBe(0);
  });
});
