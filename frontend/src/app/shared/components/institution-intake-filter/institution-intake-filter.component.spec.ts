import { TestBed } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { InstitutionIntakeFilterComponent } from './institution-intake-filter.component';
import { InstitutionService } from '../../../core/services/institution.service';
import { AcademicYearService } from '../../../core/services/academic-year.service';
import { AcademicYear, Intake } from '../../../core/models/academic-year.model';

describe('InstitutionIntakeFilterComponent', () => {
  const intake = (id: number, name: string): Intake =>
    ({ id, name, academicYearId: 1, intakeType: 'SEMESTER' } as Intake);
  const years = [
    { id: 1, name: '2027', institutionId: 5, intakes: [intake(10, 'Semester 1')] },
    { id: 2, name: '2027', institutionId: null, intakes: [intake(20, 'Annual')] },
    { id: 3, name: '2027', institutionId: 6, intakes: [intake(30, 'Trimester 1')] },
  ] as AcademicYear[];

  function build(failing = false): InstitutionIntakeFilterComponent {
    TestBed.configureTestingModule({
      imports: [InstitutionIntakeFilterComponent],
      providers: [
        provideNoopAnimations(),
        {
          provide: InstitutionService,
          useValue: {
            getInstitutions: () =>
              failing ? throwError(() => new Error('down')) : of([{ id: 5, name: 'University X' }]),
          },
        },
        { provide: AcademicYearService, useValue: { getAcademicYears: () => of(years) } },
      ],
    });
    const fixture = TestBed.createComponent(InstitutionIntakeFilterComponent);
    fixture.componentRef.setInput(
      'group',
      new FormGroup({ institutionId: new FormControl(''), intakeId: new FormControl('') })
    );
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('lists every intake in plain words when no institution is chosen', () => {
    const component = build();

    expect(component.institutions.length).toBe(1);
    expect(component.intakeChoices.map((c) => c.label)).toEqual([
      '2027 · Semester 1',
      '2027 · Annual',
      '2027 · Trimester 1',
    ]);
  });

  it('narrows intakes to the institution and "All institutions" years', () => {
    const component = build();
    component.group.get('institutionId')!.setValue(5);

    expect(component.intakeChoices.map((c) => c.id)).toEqual([10, 20]);
  });

  it('a failed institution lookup leaves that list empty', () => {
    const component = build(true);

    expect(component.institutions).toEqual([]);
    expect(component.intakeChoices.length).toBe(3);
  });
});
