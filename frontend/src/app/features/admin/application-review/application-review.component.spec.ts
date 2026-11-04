import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ApplicationReviewComponent } from './application-review.component';
import { ErrorHandlingService } from '../../../core/services/error-handling.service';
import {
  AdminService,
  ApplicationResponse,
  PageResponse,
} from '../../../core/services/admin.service';
import { BuildingResponse, BuildingService } from '../../../core/services/building.service';
import { StagedAllocationService } from '../../../core/services/staged-allocation.service';
import { ApplicationStatus } from '../../../core/models/status.model';

describe('ApplicationReviewComponent status filter', () => {
  let fixture: ComponentFixture<ApplicationReviewComponent>;
  let component: ApplicationReviewComponent;
  let adminService: jasmine.SpyObj<AdminService>;
  let staged: jasmine.SpyObj<StagedAllocationService>;
  let errorService: ErrorHandlingService;

  beforeEach(() => {
    staged = jasmine.createSpyObj<StagedAllocationService>('StagedAllocationService', [
      'proposeAllocation',
      'confirmAllocation',
      'enqueueWaitingList',
    ]);
    adminService = jasmine.createSpyObj<AdminService>('AdminService', ['searchApplications']);
    adminService.searchApplications.and.returnValue(
      of({ content: [], totalElements: 0 } as unknown as PageResponse<ApplicationResponse>)
    );

    TestBed.configureTestingModule({
      imports: [ApplicationReviewComponent, NoopAnimationsModule],
      providers: [
        { provide: AdminService, useValue: adminService },
        { provide: BuildingService, useValue: { getAllBuildingsNoPagination: () => of([]) } },
        { provide: StagedAllocationService, useValue: staged },
      ],
    });
    errorService = TestBed.inject(ErrorHandlingService);
    fixture = TestBed.createComponent(ApplicationReviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function statusChip(): HTMLElement | null {
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('.active-status-filter');
  }

  function lastSearchedStatus(): string | undefined {
    return adminService.searchApplications.calls.mostRecent().args[0].status;
  }

  it('statuses_listEveryApplicationStatus', () => {
    const offered = component.statuses.map((s) => s.value).sort();

    expect(offered).toEqual(Object.values(ApplicationStatus).sort());
  });

  it('defaultFilter_onLoad_isCountedAndVisibleWithThePanelCollapsed', () => {
    // The first call is the list. The ones after it fetch the summary counts.
    expect(adminService.searchApplications.calls.first().args[0].status).toBe('PENDING');
    expect(component.filtersVisible).toBeFalse();
    expect(component.activeFilterCount).toBe(1);
    expect(statusChip()!.textContent).toContain('Pending');
  });

  it('clearFilters_removesTheStatusFilterToo', () => {
    component.onClearFilters();

    expect(lastSearchedStatus()).toBeFalsy();
    expect(component.activeFilterCount).toBe(0);
    expect(statusChip()).toBeNull();
  });

  it('chip_showsTheFilterInUseNotOneStillBeingPicked', () => {
    component.filterForm.patchValue({ status: 'UNDER_REVIEW' });
    expect(statusChip()!.textContent).toContain('Pending');

    component.onFilter();

    expect(lastSearchedStatus()).toBe('UNDER_REVIEW');
    expect(statusChip()!.textContent).toContain('Under Review');
  });

  it('chip_cancelledStatus_readsAsAWordNotAnEnumName', () => {
    component.filterForm.patchValue({ status: 'CANCELLED' });
    component.onFilter();

    expect(statusChip()!.textContent).toContain('Cancelled');
  });

  it('computeActiveFilterCount_countsEveryFilledFilter', () => {
    component.filterForm.patchValue({
      applicationCode: 'APP',
      studentName: 'Thandi',
      status: 'PENDING',
      allocationStatus: 'PENDING',
      preferredBuilding: 'Block A',
      startDate: '2026-01-01',
      endDate: '2026-02-01',
    });

    expect(component.activeFilterCount).toBe(7);
  });

  it('loadApplications_sendsTheFiltersIncludingFormattedDates', () => {
    component.filterForm.patchValue({
      applicationCode: 'APP',
      preferredBuilding: { name: 'Block A' },
      allocationStatus: 'ALLOCATED',
      startDate: new Date(2026, 0, 5),
      endDate: '2026-02-01',
    });

    component.onFilter();

    expect(adminService.searchApplications.calls.mostRecent().args[0]).toEqual(
      jasmine.objectContaining({
        applicationCode: 'APP',
        preferredBuilding: 'Block A',
        allocationStatus: 'ALLOCATED',
        startDate: '2026-01-05',
        endDate: '2026-02-01',
        page: 0,
      })
    );
  });

  it('loadApplications_backendFails_showsTheErrorAndCanBeDismissed', () => {
    adminService.searchApplications.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500 }))
    );

    component.retryLoadApplications();

    expect(component.loadError).not.toBeNull();
    expect(component.isLoading).toBeFalse();

    component.dismissLoadError();
    expect(component.loadError).toBeNull();
  });

  it('onPageChange_loadsTheRequestedPage', () => {
    component.onPageChange({ pageIndex: 2, pageSize: 50, length: 200 });

    expect(adminService.searchApplications.calls.mostRecent().args[0]).toEqual(
      jasmine.objectContaining({ page: 2, size: 50 })
    );
  });

  it('toggleFilters_opensAndClosesThePanel', () => {
    component.toggleFilters();
    expect(component.filtersVisible).toBeTrue();
    fixture.detectChanges();

    component.toggleFilters();
    expect(component.filtersVisible).toBeFalse();
  });

  it('buildingFilter_matchesAndDisplaysBuildings', () => {
    component.buildings = [
      { name: 'Block A' } as BuildingResponse,
      { name: 'Hall B' } as BuildingResponse,
    ];

    expect(component.filterBuildings('block').map((b) => b.name)).toEqual(['Block A']);
    expect(component.displayBuilding('typed')).toBe('typed');
    expect(component.displayBuilding({ name: 'Hall B' } as BuildingResponse)).toBe('Hall B');

    component.onBuildingSelected({
      option: { value: { name: 'Hall B' } },
    } as unknown as MatAutocompleteSelectedEvent);
    expect(component.filterForm.value.preferredBuilding).toBe('Hall B');
  });

  it('displayHelpers_reflectTheApplication', () => {
    const app = { studentFirstName: 'thandi', studentLastName: 'mokoena' } as ApplicationResponse;

    expect(component.getStudentInitials(app)).toBe('TM');
    expect(component.getStudentInitials({} as ApplicationResponse)).toBe('?');
    expect(component.getStatusClass('PENDING')).toBe('pending');
    expect(component.getStatusIcon('PENDING')).toBe('pending_actions');
    expect(component.getStatusIcon('APPROVED')).toBe('check_circle');
    expect(component.getStatusIcon('REJECTED')).toBe('cancel');
    expect(component.getStatusIcon('EXPIRED')).toBe('help');
    expect(component.getScoreClass(undefined)).toBe('');
    expect(component.getScoreClass(90)).toBe('score-high');
    expect(component.getScoreClass(60)).toBe('score-medium');
    expect(component.getScoreClass(10)).toBe('score-low');
    expect(component.getScoreRingStyle(null)).toEqual({});
    expect(component.getScoreRingStyle(90)['--ring-color']).toBe('#059669');
    expect(component.getScoreRingStyle(60)['--ring-color']).toBe('#d97706');
    expect(component.getScoreRingStyle(10)['--ring-color']).toBe('#dc2626');
  });

  describe('dialogs', () => {
    let open: jasmine.Spy;

    function answerDialogWith(result: unknown): void {
      open = spyOn(fixture.debugElement.injector.get(MatDialog), 'open').and.returnValue({
        afterClosed: () => of(result),
      } as unknown as MatDialogRef<unknown>);
    }

    const app = { id: 7, studentId: 3, status: 'AWAITING_ALLOCATION' } as ApplicationResponse;

    ['allocated', 'rejected', 'saved'].forEach((action) => {
      it(`openDetailDialog_${action}_reloadsTheList`, () => {
        answerDialogWith({ action, applicationId: 7 });
        adminService.searchApplications.calls.reset();

        component.openDetailDialog(app);

        expect(adminService.searchApplications).toHaveBeenCalled();
      });
    });

    it('openDetailDialog_closedWithoutChange_keepsTheList', () => {
      answerDialogWith({ action: 'closed', applicationId: 7 });
      adminService.searchApplications.calls.reset();

      component.openDetailDialog(app);

      expect(open).toHaveBeenCalled();
      expect(adminService.searchApplications).not.toHaveBeenCalled();
    });

    it('openAllocationDialog_allocated_reloadsTheList', () => {
      answerDialogWith({ action: 'allocated' });
      adminService.searchApplications.calls.reset();

      component.openAllocationDialog(app);

      expect(adminService.searchApplications).toHaveBeenCalled();
    });

    it('openPreferencesDialog_passesTheApplication', () => {
      answerDialogWith(undefined);

      component.openPreferencesDialog(app);

      expect(open.calls.mostRecent().args[1].data).toEqual({ application: app });
    });
  });

  describe('runAutoAllocation', () => {
    const app = { id: 7, studentId: 3, status: 'AWAITING_ALLOCATION' } as ApplicationResponse;

    it('runAutoAllocation_wrongStatus_callsNothing', () => {
      component.runAutoAllocation({ ...app, status: 'PENDING' });

      expect(staged.proposeAllocation).not.toHaveBeenCalled();
    });

    it('runAutoAllocation_roomProposed_confirmsIt', () => {
      staged.proposeAllocation.and.returnValue(of({ proposals: [{ id: 9 }] }) as never);
      staged.confirmAllocation.and.returnValue(of({}) as never);

      component.runAutoAllocation(app);

      expect(staged.confirmAllocation).toHaveBeenCalledOnceWith(9);
    });

    it('runAutoAllocation_confirmFails_doesNotWaitlist', () => {
      staged.proposeAllocation.and.returnValue(of({ proposals: [{ id: 9 }] }) as never);
      staged.confirmAllocation.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 500 }))
      );

      component.runAutoAllocation(app);

      expect(staged.enqueueWaitingList).not.toHaveBeenCalled();
    });

    it('runAutoAllocation_noProposal_addsToTheWaitingList', () => {
      staged.proposeAllocation.and.returnValue(of({ proposals: [] }) as never);
      staged.enqueueWaitingList.and.returnValue(of({ message: 'Queued' }) as never);

      component.runAutoAllocation(app);

      expect(staged.enqueueWaitingList).toHaveBeenCalledOnceWith(3, 7);
    });

    it('runAutoAllocation_noRoomError_addsToTheWaitingList', () => {
      spyOn(errorService, 'parseError').and.returnValue({ message: 'No eligible rooms' } as never);
      staged.proposeAllocation.and.returnValue(throwError(() => new Error('refused')));
      staged.enqueueWaitingList.and.returnValue(throwError(() => new Error('refused')));

      component.runAutoAllocation(app);

      expect(staged.enqueueWaitingList).toHaveBeenCalledOnceWith(3, 7);
    });

    it('runAutoAllocation_otherError_doesNotWaitlist', () => {
      spyOn(errorService, 'parseError').and.returnValue({ message: 'Server exploded' } as never);
      staged.proposeAllocation.and.returnValue(throwError(() => new Error('refused')));

      component.runAutoAllocation(app);

      expect(staged.enqueueWaitingList).not.toHaveBeenCalled();
    });
  });
});
