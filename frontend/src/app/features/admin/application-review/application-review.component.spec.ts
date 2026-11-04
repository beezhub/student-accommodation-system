import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { ApplicationReviewComponent } from './application-review.component';
import { AdminService, ApplicationResponse } from '../../../core/services/admin.service';
import { BuildingService } from '../../../core/services/building.service';

/** The real shape of the backend ApplicationResponse: one `studentName`, no first and last. */
const APPLICATION = {
  id: 1,
  applicationCode: 'APP-1',
  status: 'PENDING',
  studentId: 9,
  studentName: 'Thandi van der Merwe',
  studentEmail: 'thandi@example.com',
} as unknown as ApplicationResponse;

describe('ApplicationReviewComponent', () => {
  function render() {
    TestBed.configureTestingModule({
      imports: [ApplicationReviewComponent],
      providers: [
        provideNoopAnimations(),
        {
          provide: AdminService,
          useValue: jasmine.createSpyObj('AdminService', {
            searchApplications: of({ content: [APPLICATION], totalElements: 1 }),
          }),
        },
        {
          provide: BuildingService,
          useValue: jasmine.createSpyObj('BuildingService', {
            getAllBuildingsNoPagination: of([]),
          }),
        },
      ],
    });
    const fixture = TestBed.createComponent(ApplicationReviewComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('applicationList_responseWithStudentName_showsTheNameAndInitials', () => {
    const page: HTMLElement = render().nativeElement;

    expect(page.querySelector('.student-name')?.textContent?.trim()).toBe('Thandi van der Merwe');
    expect(page.querySelector('.mc-student-name')?.textContent?.trim()).toBe(
      'Thandi van der Merwe'
    );
    expect(page.querySelector('.student-cell .avatar-circle')?.textContent?.trim()).toBe('TM');
  });
});
