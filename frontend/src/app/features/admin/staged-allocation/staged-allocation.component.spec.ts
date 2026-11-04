import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { StagedAllocationComponent } from './staged-allocation.component';
import { AdminService, ApplicationResponse } from '../../../core/services/admin.service';
import { StagedAllocationService } from '../../../core/services/staged-allocation.service';
import { StagedAllocation } from '../../../core/models/allocation.models';

/** The real shape of the backend ApplicationResponse: one `studentName`, no first and last. */
const APPLICATION = {
  id: 1,
  applicationCode: 'APP-1',
  status: 'AWAITING_ALLOCATION',
  studentId: 9,
  studentName: 'Thandi Mokoena',
  studentEmail: 'thandi@example.com',
} as unknown as ApplicationResponse;

function proposal(id: number, status: StagedAllocation['status']): StagedAllocation {
  return {
    id,
    status,
    batchId: 'BATCH-1',
    warnings: [],
    score: 100,
  } as unknown as StagedAllocation;
}

describe('StagedAllocationComponent', () => {
  function setup() {
    const staged = jasmine.createSpyObj('StagedAllocationService', [
      'getExistingProposals',
      'proposeAllocation',
      'confirmBatch',
      'getProposals',
    ]);
    staged.getExistingProposals.and.returnValue(of({ content: [] }));
    const snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
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
        { provide: StagedAllocationService, useValue: staged },
      ],
    });
    // The component imports MatSnackBarModule itself, so the spy has to be an override.
    TestBed.overrideProvider(MatSnackBar, { useValue: snackBar });
    const fixture = TestBed.createComponent(StagedAllocationComponent);
    fixture.detectChanges();
    return {
      fixture,
      component: fixture.componentInstance,
      page: fixture.nativeElement as HTMLElement,
      staged,
      snackBar,
    };
  }

  it('selectStudentsStep_responseWithStudentName_showsTheNameAndInitials', () => {
    const { page } = setup();

    expect(page.querySelector('.student-name')?.textContent?.trim()).toBe('Thandi Mokoena');
    expect(page.querySelector('.student-cell .avatar-circle')?.textContent?.trim()).toBe('TM');
  });

  it('confirmAllRemaining_partialFailure_marksOnlyConfirmedRowsAndShowsErrors', () => {
    const { fixture, component, page, staged, snackBar } = setup();
    component.batchId = 'BATCH-1';
    component.proposals = [proposal(11, 'PROPOSED'), proposal(12, 'PROPOSED')];
    // 501 is what the API really sends: the id of the new room allocation, not the staged row.
    staged.confirmBatch.and.returnValue(
      of({
        success: false,
        totalAttempted: 2,
        successCount: 1,
        failureCount: 1,
        errors: ['Staged allocation 12: Room is full'],
        confirmedAllocationIds: [501],
      })
    );
    staged.getProposals.and.returnValue(
      of({ content: [proposal(11, 'CONFIRMED'), proposal(12, 'PROPOSED')] })
    );

    component.confirmAllRemaining();
    fixture.detectChanges();

    expect(component.proposals.map((p) => p.status)).toEqual(['CONFIRMED', 'PROPOSED']);
    expect(component.confirmedCount).toBe(1);
    expect(snackBar.open).toHaveBeenCalledWith(
      'Confirmed 1 of 2 allocations, 1 failed',
      'Close',
      jasmine.anything()
    );
    expect(page.querySelector('.confirm-errors')?.textContent).toContain(
      'Staged allocation 12: Room is full'
    );
  });

  it('confirmAllRemaining_allConfirmed_reportsTheRealCountAndNoErrors', () => {
    const { fixture, component, page, staged, snackBar } = setup();
    component.batchId = 'BATCH-1';
    component.proposals = [proposal(11, 'PROPOSED')];
    // errors is left out of the JSON when there are none (JsonInclude NON_NULL).
    staged.confirmBatch.and.returnValue(
      of({ success: true, totalAttempted: 1, successCount: 1, failureCount: 0 })
    );
    staged.getProposals.and.returnValue(of({ content: [proposal(11, 'CONFIRMED')] }));

    component.confirmAllRemaining();
    fixture.detectChanges();

    expect(component.proposals[0].status).toBe('CONFIRMED');
    expect(snackBar.open).toHaveBeenCalledWith(
      'Confirmed 1 allocation',
      'Close',
      jasmine.anything()
    );
    expect(page.querySelector('.confirm-errors')).toBeNull();
  });

  it('proposeBatch_responseWithFailuresAndWarnings_showsThem', () => {
    const { fixture, component, page, staged } = setup();
    component.selection.select(APPLICATION);
    staged.proposeAllocation.and.returnValue(
      of({
        batchId: 'BATCH-1',
        proposals: [],
        warnings: ['Student Sipho Dlamini (ID: 8) already has a pending proposal, skipped'],
        failures: [{ studentId: 9, studentName: 'Thandi Mokoena', reason: 'No suitable room' }],
      })
    );

    component.proposeBatch();
    fixture.detectChanges();

    expect(page.querySelector('.proposal-failures')?.textContent).toContain(
      'Thandi Mokoena: No suitable room'
    );
    expect(page.querySelector('.proposal-warnings')?.textContent).toContain(
      'Sipho Dlamini (ID: 8) already has a pending proposal'
    );
  });
});
