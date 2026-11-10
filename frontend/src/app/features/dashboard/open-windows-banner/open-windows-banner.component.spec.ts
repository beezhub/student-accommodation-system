import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { OpenWindowsBannerComponent } from './open-windows-banner.component';
import { ApplyOptionsService } from '../../../core/services/apply-options.service';
import { CompanyApplyOptions } from '../../../core/models/apply-options.model';

describe('OpenWindowsBannerComponent', () => {
  const window: CompanyApplyOptions = {
    companyId: 1,
    companyName: 'Company A',
    openIntakes: [
      {
        intakeId: 5,
        intakeName: 'Semester 2',
        academicYearId: 2,
        academicYearName: '2027',
        stayStartDate: '2027-07-15',
        stayEndDate: '2027-11-30',
        applicationOpenDate: '2027-05-01',
        applicationCloseDate: '2027-06-30',
      },
    ],
    existingApplications: [],
    missingDocuments: [],
  };

  const create = (getOpenWindows: () => unknown) => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: ApplyOptionsService, useValue: { getOpenWindows } },
      ],
    });
    const fixture = TestBed.createComponent(OpenWindowsBannerComponent);
    fixture.detectChanges();
    return fixture;
  };

  it('openWindow_showsTheCompanyAndTheIntakeInPlainWords', () => {
    const fixture = create(() => of([window]));

    expect(fixture.nativeElement.textContent).toContain(
      'Company A: Semester 2, 2027. Move in 15 July. Apply by 30 June.'
    );
  });

  it('nothingOpenOrLookupFails_showsNothing', () => {
    const fixture = create(() => throwError(() => new Error('down')));

    expect(fixture.nativeElement.querySelector('.open-windows')).toBeNull();
  });
});
