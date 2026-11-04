import { By } from '@angular/platform-browser';
import { MatSelect } from '@angular/material/select';
import { NgControl } from '@angular/forms';
import { of } from 'rxjs';
import { ActivityLogService } from '../../../core/services/activity-log.service';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { ActivityLogsComponent } from './activity-logs.component';

/**
 * GH-462: the picker builds a Date at local midnight, and `toISOString` converts to UTC first, so
 * in South Africa a picked day was sent as the day before.
 *
 * The stub makes `toISOString` answer as it does there, so these fail on a machine in any zone. The
 * real service runs against a fake backend, so they assert the query the backend actually gets.
 */
describe('ActivityLogsComponent', () => {
  let http: HttpTestingController;
  let component: ActivityLogsComponent;

  const pickedDay = (): Date => {
    const picked = new Date(2026, 2, 15);
    spyOn(picked, 'toISOString').and.returnValue('2026-03-14T22:00:00.000Z');
    return picked;
  };

  const expectLogsRequest = () =>
    http.expectOne((request) => request.url.endsWith('/admin/activity-logs'));

  const expectExportRequest = () =>
    http.expectOne((request) => request.url.endsWith('/admin/activity-logs/export'));

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ActivityLogsComponent],
      providers: [provideNoopAnimations(), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    component = TestBed.createComponent(ActivityLogsComponent).componentInstance;
  });

  afterEach(() => http.verify());

  it('loadLogs_pickedDates_sendsTheDaysThatWerePicked', () => {
    component.filterForm.patchValue({ startDate: pickedDay(), endDate: pickedDay() });

    component.loadLogs();

    const { params } = expectLogsRequest().request;
    expect(params.get('startDate')).toBe('2026-03-15');
    expect(params.get('endDate')).toBe('2026-03-15');
  });

  it('loadLogs_noDatesPicked_leavesThemOut', () => {
    component.loadLogs();

    const { params } = expectLogsRequest().request;
    expect(params.has('startDate')).toBeFalse();
    expect(params.has('endDate')).toBeFalse();
  });

  it('onClearFilters_datesWerePicked_leavesThemOut', () => {
    component.filterForm.patchValue({ startDate: pickedDay(), endDate: pickedDay() });

    component.onClearFilters();

    const { params } = expectLogsRequest().request;
    expect(params.has('startDate')).toBeFalse();
    expect(params.has('endDate')).toBeFalse();
  });

  it('exportLogs_pickedDates_sendsTheDaysThatWerePicked', () => {
    component.filterForm.patchValue({ startDate: pickedDay(), endDate: pickedDay() });

    component.exportLogs();

    const { params } = expectExportRequest().request;
    expect(params.get('startDate')).toBe('2026-03-15');
    expect(params.get('endDate')).toBe('2026-03-15');
  });

  it('exportLogs_justAfterMidnight_namesTheFileForToday', () => {
    const anchor = document.createElement('a');
    spyOn(anchor, 'click');
    spyOn(document, 'createElement').and.returnValue(anchor);
    spyOn(Date.prototype, 'toISOString').and.returnValue('2026-03-14T22:30:00.000Z');
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date(2026, 2, 15, 0, 30));

    try {
      component.exportLogs();
      expectExportRequest().flush(new Blob(['csv']));
    } finally {
      jasmine.clock().uninstall();
    }

    expect(anchor.download).toBe('activity-logs-2026-03-15.csv');
  });

  it('loadLogs_serverError_stopsLoading', () => {
    component.loadLogs();

    expectLogsRequest().flush('boom', { status: 500, statusText: 'Server Error' });

    expect(component.isLoading).toBeFalse();
  });
});

describe('ActivityLogsComponent role filter', () => {
  it('roleFilter_everyOption_isARoleTheApiAccepts', () => {
    TestBed.configureTestingModule({
      imports: [ActivityLogsComponent],
      providers: [
        provideNoopAnimations(),
        {
          provide: ActivityLogService,
          useValue: jasmine.createSpyObj('ActivityLogService', {
            getActivityLogs: of({ content: [], totalElements: 0 }),
            getActionIcon: 'info',
            getActionColor: '',
          }),
        },
      ],
    });
    const fixture = TestBed.createComponent(ActivityLogsComponent);
    fixture.componentInstance.filtersVisible = true;
    fixture.detectChanges();

    const select = fixture.debugElement
      .queryAll(By.directive(MatSelect))
      .find((candidate) => candidate.injector.get(NgControl).name === 'userRole')!
      .componentInstance as MatSelect;

    // The UserRole names on the backend. BUILDING_OWNER is not one, and made it answer 409.
    expect(select.options.map((option) => option.value).filter(Boolean)).toEqual([
      'PLATFORM_ADMIN',
      'PLATFORM_STAFF',
      'TENANT_ADMIN',
      'BUILDING_STAFF',
      'STUDENT',
    ]);
  });
});
