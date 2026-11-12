import { TestBed } from '@angular/core/testing';
import { AdminDashboardComponent } from './admin-dashboard.component';
import { AdminService, DashboardStats } from '../../../core/services/admin.service';
import { ApplicationStatus, emptyApplicationStatusCounts } from '../../../core/models/status.model';

describe('AdminDashboardComponent application chart', () => {
  function chartFor(byStatus: Partial<Record<ApplicationStatus, number>>, total: number) {
    TestBed.configureTestingModule({
      imports: [AdminDashboardComponent],
      providers: [{ provide: AdminService, useValue: {} }],
    });
    const component = TestBed.createComponent(AdminDashboardComponent).componentInstance;
    component.stats = {
      ...component.stats,
      totalApplications: total,
      applicationsByStatus: { ...emptyApplicationStatusCounts(), ...byStatus },
    } as DashboardStats;
    component.updateChartData();
    return component.applicationChartSegments;
  }

  it('updateChartData_checkedInApplication_isListedSoTheLegendAddsUpToTheTotal', () => {
    const segments = chartFor(
      {
        [ApplicationStatus.PENDING]: 1,
        [ApplicationStatus.ALLOCATED]: 1,
        [ApplicationStatus.CHECKED_IN]: 1,
      },
      3
    );

    expect(segments.map((s) => s.label)).toEqual(['Pending', 'Allocated', 'Checked In']);
    expect(segments.reduce((sum, s) => sum + s.value, 0)).toBe(3);
  });

  it('updateChartData_everyStatus_listsEachWithAReadableLabel', () => {
    const all = Object.fromEntries(Object.values(ApplicationStatus).map((s) => [s, 1]));

    const segments = chartFor(all, 10);

    expect(segments.length).toBe(10);
    expect(segments.map((s) => s.label)).toContain('Changes Requested');
    expect(segments.map((s) => s.label)).toContain('Awaiting Allocation');
    expect(segments.reduce((sum, s) => sum + s.percent, 0)).toBeCloseTo(100);
  });

  it('updateChartData_zeroStatuses_areLeftOut', () => {
    expect(chartFor({}, 0)).toEqual([]);
  });
});
