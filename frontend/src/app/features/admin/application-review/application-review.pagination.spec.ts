import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { MatPaginator } from '@angular/material/paginator';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Subject, of } from 'rxjs';

import { ApplicationReviewComponent } from './application-review.component';
import { AdminService } from '../../../core/services/admin.service';
import { BuildingService } from '../../../core/services/building.service';

const PAGE = {
  content: [{ id: 1, applicationCode: 'APP-1', status: 'PENDING', studentId: 9 }],
  totalElements: 100,
};

describe('ApplicationReviewComponent paging', () => {
  function setup() {
    const list = new Subject<typeof PAGE>();
    const admin = jasmine.createSpyObj('AdminService', ['searchApplications']);
    // The status counters ask for one row at a time and answer at once; the list waits for the
    // test, so isLoading really is true for a render and the paginator really is destroyed.
    admin.searchApplications.and.callFake((filters: { size: number }) =>
      filters.size === 1 ? of(PAGE) : list
    );
    TestBed.configureTestingModule({
      imports: [ApplicationReviewComponent],
      providers: [
        provideNoopAnimations(),
        { provide: AdminService, useValue: admin },
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

    const finishLoading = () => {
      list.next(PAGE);
      fixture.detectChanges();
    };
    const paginator = () =>
      fixture.debugElement.query(By.directive(MatPaginator)).componentInstance as MatPaginator;

    finishLoading();
    return { fixture, component: fixture.componentInstance, finishLoading, paginator };
  }

  it('paginator_afterLoadingALaterPage_staysOnThatPage', () => {
    const { fixture, component, finishLoading, paginator } = setup();

    component.onPageChange({ pageIndex: 2, pageSize: 20, length: 100 });
    fixture.detectChanges();
    finishLoading();

    expect(paginator().pageIndex).toBe(2);
  });

  it('paginator_afterFiltering_returnsToTheFirstPage', () => {
    const { fixture, component, finishLoading, paginator } = setup();
    component.onPageChange({ pageIndex: 2, pageSize: 20, length: 100 });
    fixture.detectChanges();
    finishLoading();

    component.onFilter();
    fixture.detectChanges();
    finishLoading();

    expect(paginator().pageIndex).toBe(0);
  });
});
