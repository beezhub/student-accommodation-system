import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { StagedAllocationComponent } from './staged-allocation.component';
import { AdminService, ApplicationResponse } from '../../../core/services/admin.service';
import { StagedAllocationService } from '../../../core/services/staged-allocation.service';

/** The real shape of the backend ApplicationResponse: one `studentName`, no first and last. */
const APPLICATION = {
  id: 1,
  applicationCode: 'APP-1',
  status: 'AWAITING_ALLOCATION',
  studentId: 9,
  studentName: 'Thandi Mokoena',
  studentEmail: 'thandi@example.com',
} as unknown as ApplicationResponse;

describe('StagedAllocationComponent', () => {
  function render() {
    TestBed.configureTestingModule({
      imports: [StagedAllocationComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        {
          provide: AdminService,
          useValue: jasmine.createSpyObj('AdminService', {
            searchApplications: of({ content: [APPLICATION], totalElements: 1 }),
          }),
        },
        {
          provide: StagedAllocationService,
          useValue: jasmine.createSpyObj('StagedAllocationService', {
            getExistingProposals: of({ content: [] }),
          }),
        },
      ],
    });
    const fixture = TestBed.createComponent(StagedAllocationComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('selectStudentsStep_responseWithStudentName_showsTheNameAndInitials', () => {
    const page: HTMLElement = render().nativeElement;

    expect(page.querySelector('.student-name')?.textContent?.trim()).toBe('Thandi Mokoena');
    expect(page.querySelector('.student-cell .avatar-circle')?.textContent?.trim()).toBe('TM');
  });
});
