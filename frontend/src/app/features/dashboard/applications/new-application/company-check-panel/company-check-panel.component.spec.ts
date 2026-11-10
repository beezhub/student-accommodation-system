import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { CompanyCheckPanelComponent } from './company-check-panel.component';
import { CompanyCheck } from '../company-checks';
import { IntakeOption } from '../../../../../core/models/apply-options.model';

describe('CompanyCheckPanelComponent', () => {
  const intake = (intakeId: number, intakeName: string): IntakeOption => ({
    intakeId,
    intakeName,
    academicYearId: 2,
    academicYearName: '2027',
    stayStartDate: '2027-07-15',
    stayEndDate: '2027-11-30',
    applicationOpenDate: '2027-05-01',
    applicationCloseDate: '2027-06-30',
  });
  const check = (extra: Partial<CompanyCheck['options']> = {}): CompanyCheck => ({
    companyId: 1,
    companyName: 'Company B',
    buildingIds: [2],
    loading: false,
    loadError: null,
    chosenIntakeId: 5,
    options: {
      companyId: 1,
      companyName: 'Company B',
      openIntakes: [intake(5, 'Semester 2')],
      existingApplications: [],
      missingDocuments: [],
      ...extra,
    },
  });

  const render = (value: CompanyCheck) => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideNoopAnimations()] });
    const fixture = TestBed.createComponent(CompanyCheckPanelComponent);
    fixture.componentRef.setInput('check', value);
    fixture.componentRef.setInput('buildings', [{ id: 2, name: 'Residence 2' }]);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  it('oneOpenIntake_isShownInPlainWordsWithoutAQuestion', () => {
    const el = render(check());

    expect(el.textContent).toContain('Residence 2');
    expect(el.textContent).toContain('Semester 2, 2027. Move in 15 July. Apply by 30 June.');
    expect(el.querySelector('mat-select')).toBeNull();
  });

  it('severalOpenIntakes_asksWhichOne', () => {
    const el = render(check({ openIntakes: [intake(5, 'Semester 2'), intake(6, 'Annual')] }));

    expect(el.querySelector('mat-select')).not.toBeNull();
  });

  it('existingApplication_linksToIt', () => {
    const el = render(
      check({
        openIntakes: [],
        existingApplications: [
          {
            applicationId: 44,
            applicationCode: 'A',
            academicYearName: '2027',
            intakeName: 'Annual',
            status: 'UNDER_REVIEW',
          },
        ],
      })
    );

    expect(el.textContent).toContain('You already applied for 2027 here (Annual, under review).');
    expect(el.querySelector('a[href="/applications/44"]')).not.toBeNull();
  });

  it('missingDocuments_linksToTheDocumentsPage', () => {
    const el = render(check({ missingDocuments: ['Proof of address'] }));

    expect(el.textContent).toContain('Company B needs proof of address.');
    expect(el.querySelector('a[href="/documents"]')).not.toBeNull();
  });
});
