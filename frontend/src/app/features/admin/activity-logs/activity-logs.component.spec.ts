import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { MatSelect } from '@angular/material/select';
import { NgControl } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { ActivityLogsComponent } from './activity-logs.component';
import { ActivityLogService } from '../../../core/services/activity-log.service';

describe('ActivityLogsComponent', () => {
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
