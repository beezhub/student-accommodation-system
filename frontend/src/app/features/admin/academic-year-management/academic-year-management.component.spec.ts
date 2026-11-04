import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { AcademicYearManagementComponent } from './academic-year-management.component';
import { AcademicYear } from '../../../core/models/academic-year.model';
import { AcademicYearService } from '../../../core/services/academic-year.service';
import { RoleService } from '../../../core/services/role.service';

describe('AcademicYearManagementComponent', () => {
  const year = {
    id: 1,
    name: '2027',
    startDate: '2027-02-01',
    endDate: '2027-11-30',
    applicationOpenDate: '2026-09-01',
    applicationCloseDate: '2027-01-31',
    isCurrent: true,
    isApplicationWindowOpen: false,
  } as AcademicYear;

  function render(role: 'platformAdmin' | 'platformStaff' | 'tenantAdmin', years = [year]) {
    TestBed.configureTestingModule({
      imports: [AcademicYearManagementComponent],
      providers: [
        provideNoopAnimations(),
        {
          provide: AcademicYearService,
          useValue: jasmine.createSpyObj('AcademicYearService', {
            getAcademicYears: of(years),
          }),
        },
        {
          provide: RoleService,
          useValue: {
            isPlatformAdmin: () => role === 'platformAdmin',
            isPlatformStaff: () => role === 'platformStaff',
          },
        },
      ],
    });
    const fixture: ComponentFixture<AcademicYearManagementComponent> = TestBed.createComponent(
      AcademicYearManagementComponent
    );
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('render_platformAdmin_offersAddAndRowActions', () => {
    const page = render('platformAdmin');

    expect(page.querySelector('.add-btn')).not.toBeNull();
    expect(page.querySelector('.action-btn.set-current')).not.toBeNull();
  });

  it('render_platformStaff_offersAddAndRowActions', () => {
    const page = render('platformStaff');

    expect(page.querySelector('.add-btn')).not.toBeNull();
    expect(page.querySelector('.action-btn.set-current')).not.toBeNull();
  });

  it('render_tenantAdmin_listsYearsWithNoWriteActions', () => {
    const page = render('tenantAdmin');

    expect(page.querySelector('.year-name')?.textContent).toContain('2027');
    expect(page.querySelector('.add-btn')).toBeNull();
    expect(page.querySelector('.action-btn')).toBeNull();
  });

  it('render_tenantAdmin_saysThePlatformSetsTheCalendar', () => {
    const page = render('tenantAdmin');

    expect(page.querySelector('.hero-subtitle')?.textContent).toContain('set by the platform');
  });

  it('render_tenantAdminWithNoYears_doesNotTellThemToAddOne', () => {
    const page = render('tenantAdmin', []);

    expect(page.querySelector('app-empty-state')?.textContent).not.toContain('Add one');
  });
});
