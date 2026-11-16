import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatSelect } from '@angular/material/select';
import { of, throwError } from 'rxjs';

import { AllocationApplicationFieldComponent } from './allocation-application-field.component';
import { AdminService, ApplicationResponse } from '../../../../../core/services/admin.service';

describe('AllocationApplicationFieldComponent', () => {
  const application = (id: number): ApplicationResponse =>
    ({ id, applicationCode: `APP-2027-${id}`, status: 'APPROVED' } as ApplicationResponse);

  let searchApplications: jasmine.Spy;
  let fixture: ComponentFixture<AllocationApplicationFieldComponent>;
  let component: AllocationApplicationFieldComponent;
  let group: FormGroup;

  beforeEach(() => {
    searchApplications = jasmine
      .createSpy('searchApplications')
      .and.returnValue(of({ content: [application(11), application(12)], totalElements: 2 }));
    spyOn(console, 'error');
    TestBed.configureTestingModule({
      imports: [AllocationApplicationFieldComponent],
      providers: [
        provideNoopAnimations(),
        { provide: AdminService, useValue: { searchApplications } },
      ],
    });
  });

  function build(): void {
    group = new FormGroup({ applicationId: new FormControl<number | ''>('') });
    fixture = TestBed.createComponent(AllocationApplicationFieldComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('group', group);
    fixture.detectChanges();
  }

  it('ngOnInit_firstPage_listsApprovedApplications', () => {
    build();

    expect(searchApplications).toHaveBeenCalledWith({
      status: 'APPROVED',
      page: 0,
      size: 25,
      applicationCode: undefined,
    });
    expect(component.studentApplications.map((a) => a.id)).toEqual([11, 12]);
    expect(component.moreApplicationsExist).toBeFalse();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Link this allocation to an existing application'
    );
  });

  it('ngOnInit_morePagesExist_saysSo', () => {
    searchApplications.and.returnValue(of({ content: [application(11)], totalElements: 30 }));
    build();

    expect(component.moreApplicationsExist).toBeTrue();
  });

  it('applicationSearch_typedCode_searchesTheServerAfterAPause', fakeAsync(() => {
    build();
    searchApplications.calls.reset();

    component.applicationSearch.setValue('APP-2027');
    tick(299);
    expect(searchApplications).not.toHaveBeenCalled();
    tick(1);

    expect(searchApplications).toHaveBeenCalledWith({
      status: 'APPROVED',
      page: 0,
      size: 25,
      applicationCode: 'APP-2027',
    });
  }));

  it('loadApplications_serverError_leavesTheListEmpty', () => {
    searchApplications.and.returnValue(throwError(() => new Error('down')));
    build();

    expect(component.studentApplications).toEqual([]);
    expect(console.error).toHaveBeenCalled();
  });

  it('group_applicationIdControl_isTheSelectValue', () => {
    build();
    group.get('applicationId')!.setValue(12);
    fixture.detectChanges();

    const select = fixture.debugElement.query(By.directive(MatSelect)).componentInstance;
    expect((select as MatSelect).value).toBe(12);
  });
});
