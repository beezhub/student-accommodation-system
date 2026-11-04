import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { ApplicationReviewComponent } from './application-review.component';
import {
  AdminService,
  ApplicationResponse,
  PageResponse,
} from '../../../core/services/admin.service';
import { BuildingService } from '../../../core/services/building.service';
import { StagedAllocationService } from '../../../core/services/staged-allocation.service';
import { ApplicationStatus } from '../../../core/models/status.model';

describe('ApplicationReviewComponent status filter', () => {
  let fixture: ComponentFixture<ApplicationReviewComponent>;
  let component: ApplicationReviewComponent;
  let adminService: jasmine.SpyObj<AdminService>;

  beforeEach(() => {
    adminService = jasmine.createSpyObj<AdminService>('AdminService', ['searchApplications']);
    adminService.searchApplications.and.returnValue(
      of({ content: [], totalElements: 0 } as unknown as PageResponse<ApplicationResponse>)
    );

    TestBed.configureTestingModule({
      imports: [ApplicationReviewComponent, NoopAnimationsModule],
      providers: [
        { provide: AdminService, useValue: adminService },
        { provide: BuildingService, useValue: { getAllBuildingsNoPagination: () => of([]) } },
        { provide: StagedAllocationService, useValue: {} },
      ],
    });
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
});
