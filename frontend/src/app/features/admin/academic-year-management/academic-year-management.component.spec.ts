import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { AcademicYearManagementComponent } from './academic-year-management.component';
import { AcademicYear } from '../../../core/models/academic-year.model';
import { AcademicYearService } from '../../../core/services/academic-year.service';
import { RoleService } from '../../../core/services/role.service';

describe('AcademicYearManagementComponent', () => {
  const allInstitutions: AcademicYear = {
    id: 1,
    name: '2027',
    institutionId: null,
    startDate: '2027-02-01',
    endDate: '2027-11-30',
    applicationOpenDate: '2026-10-04',
    applicationCloseDate: '2027-01-18',
    isApplicationWindowOpen: false,
    intakes: [
      {
        id: 10,
        academicYearId: 1,
        intakeType: 'ANNUAL',
        name: 'Annual',
        stayStartDate: '2027-02-01',
        stayEndDate: '2027-11-30',
        applicationOpenDate: '2026-10-04',
        applicationCloseDate: '2027-01-18',
        isApplicationWindowOpen: true,
      },
    ],
  };
  const universityX: AcademicYear = {
    ...allInstitutions,
    id: 2,
    name: 'UX 2027',
    institutionId: 7,
    institutionName: 'University X',
    intakes: [],
  };

  let service: jasmine.SpyObj<AcademicYearService>;

  function render(
    role: 'platformAdmin' | 'tenantAdmin' | 'buildingStaff',
    years: AcademicYear[] = [allInstitutions, universityX]
  ) {
    service = jasmine.createSpyObj('AcademicYearService', {
      getAcademicYears: of(years),
      copyAcademicYear: of({ ...allInstitutions, id: 3, name: '2028' }),
    });
    TestBed.configureTestingModule({
      imports: [AcademicYearManagementComponent],
      providers: [
        provideNoopAnimations(),
        { provide: AcademicYearService, useValue: service },
        {
          provide: RoleService,
          useValue: {
            isPlatformAdmin: () => role === 'platformAdmin',
            isPlatformStaff: () => false,
            isTenantAdmin: () => role === 'tenantAdmin',
          },
        },
      ],
    });
    const fixture: ComponentFixture<AcademicYearManagementComponent> = TestBed.createComponent(
      AcademicYearManagementComponent
    );
    fixture.detectChanges();
    return fixture;
  }

  /** GH-439: a company admin manages their own years and has no set-current action. */
  it('render_tenantAdmin_offersCreateCopyAndEditButNoSetCurrent', () => {
    const page = render('tenantAdmin').nativeElement as HTMLElement;

    expect(page.querySelector('.add-btn')).not.toBeNull();
    expect(page.querySelector('.copy-btn')).not.toBeNull();
    expect(page.querySelector('.action-btn.edit')).not.toBeNull();
    expect(page.querySelector('.set-current')).toBeNull();
    expect(page.textContent).not.toContain('Set as Current');
  });

  it('render_groupsYearsByInstitution_allInstitutionsFirst', () => {
    const page = render('platformAdmin').nativeElement as HTMLElement;

    const titles = Array.from(page.querySelectorAll('.group-title')).map((e) =>
      e.textContent?.trim()
    );
    expect(titles).toEqual(['All institutions', 'University X']);
  });

  it('render_listsIntakesInPlainWords', () => {
    const page = render('tenantAdmin').nativeElement as HTMLElement;

    expect(page.querySelector('.intake-text')?.textContent).toContain(
      'Annual, 2027. Move in 1 February.'
    );
  });

  it('render_withoutAWriteRole_showsNoWriteActions', () => {
    const page = render('buildingStaff').nativeElement as HTMLElement;

    expect(page.querySelector('.add-btn')).toBeNull();
    expect(page.querySelector('.action-btn')).toBeNull();
    expect(page.querySelector('.intake-edit')).toBeNull();
  });

  it('copyLastYear_copiesTheGroupsNewestYearAndReloads', () => {
    const fixture = render('tenantAdmin');

    fixture.componentInstance.copyLastYear(fixture.componentInstance.groups[0]);

    expect(service.copyAcademicYear).toHaveBeenCalledWith(1);
    expect(service.getAcademicYears).toHaveBeenCalledTimes(2);
  });

  it('render_noYears_saysHowToStart', () => {
    const page = render('tenantAdmin', []).nativeElement as HTMLElement;

    expect(page.querySelector('app-empty-state')).not.toBeNull();
  });
});
