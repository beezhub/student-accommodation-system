import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ActivityLogFilters } from '../models/activity-log.model';
import { ActivityLogService } from './activity-log.service';

describe('ActivityLogService', () => {
  const filters: ActivityLogFilters = {
    userId: 7,
    userRole: 'STUDENT',
    action: 'LOGIN',
    entityType: 'USER',
    startDate: '2026-03-15',
    endDate: '2026-03-16',
  };

  let service: ActivityLogService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ActivityLogService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const expectEveryFilter = (params: { get(name: string): string | null }): void => {
    expect(params.get('userId')).toBe('7');
    expect(params.get('userRole')).toBe('STUDENT');
    expect(params.get('action')).toBe('LOGIN');
    expect(params.get('entityType')).toBe('USER');
    expect(params.get('startDate')).toBe('2026-03-15');
    expect(params.get('endDate')).toBe('2026-03-16');
  };

  it('getActivityLogs_everyFilterSet_sendsThemAll', () => {
    service.getActivityLogs({ ...filters, page: 2, size: 50 }).subscribe();

    const { params } = http.expectOne((r) => r.url.endsWith('/admin/activity-logs')).request;
    expectEveryFilter(params);
    expect(params.get('page')).toBe('2');
    expect(params.get('size')).toBe('50');
  });

  it('getActivityLogs_noFilters_sendsOnlyTheDefaultPage', () => {
    service.getActivityLogs().subscribe();

    const { params } = http.expectOne((r) => r.url.endsWith('/admin/activity-logs')).request;
    expect(params.keys().sort()).toEqual(['page', 'size']);
    expect(params.get('page')).toBe('0');
    expect(params.get('size')).toBe('20');
  });

  it('exportActivityLogs_everyFilterSet_sendsThemAll', () => {
    service.exportActivityLogs(filters).subscribe();

    expectEveryFilter(http.expectOne((r) => r.url.endsWith('/export')).request.params);
  });

  it('exportActivityLogs_noFilters_sendsNoParams', () => {
    service.exportActivityLogs().subscribe();

    expect(http.expectOne((r) => r.url.endsWith('/export')).request.params.keys()).toEqual([]);
  });

  it('getActionIcon_unknownAction_fallsBack', () => {
    expect(service.getActionIcon('LOGIN')).toBe('login');
    expect(service.getActionIcon('NOPE')).toBe('info');
  });

  it('getActionColor_unknownAction_fallsBack', () => {
    expect(service.getActionColor('DELETE')).toBe('warn');
    expect(service.getActionColor('NOPE')).toBe('neutral');
  });
});
