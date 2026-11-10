import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnInit,
  inject,
} from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { InstitutionService } from '../../../core/services/institution.service';
import { AcademicYearService } from '../../../core/services/academic-year.service';
import { Institution } from '../../../core/models/institution.model';
import { AcademicYear } from '../../../core/models/academic-year.model';

/** One intake as a choice, for example "2027 · Semester 2". */
export interface IntakeChoice {
  id: number;
  label: string;
  institutionId: number | null;
}

/**
 * Institution and intake filters for the admin applications and allocations lists (GH-496).
 * Binds to the parent's filter form, which must have `institutionId` and `intakeId` controls. The
 * intakes are the company's own, and narrow to the chosen institution (plus "All institutions"
 * years). A failed lookup leaves the lists empty rather than breaking the page.
 */
@Component({
  selector: 'app-institution-intake-filter',
  templateUrl: './institution-intake-filter.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatSelectModule],
  // The two fields sit in the parent's filter grid as if they were its own.
  host: { style: 'display: contents' },
})
export class InstitutionIntakeFilterComponent implements OnInit {
  private readonly institutionService = inject(InstitutionService);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly cdr = inject(ChangeDetectorRef);

  @Input({ required: true }) group!: FormGroup;

  institutions: Institution[] = [];
  private intakes: IntakeChoice[] = [];

  ngOnInit(): void {
    forkJoin({
      institutions: this.institutionService.getInstitutions().pipe(catchError(() => of([]))),
      years: this.academicYearService.getAcademicYears(true).pipe(catchError(() => of([]))),
    }).subscribe(({ institutions, years }) => {
      this.institutions = institutions;
      this.intakes = InstitutionIntakeFilterComponent.toChoices(years);
      this.cdr.markForCheck();
    });
  }

  /** The intakes for the chosen institution, or all of them when none is chosen. */
  get intakeChoices(): IntakeChoice[] {
    const institutionId = this.group.get('institutionId')?.value as number | null | '';
    if (!institutionId) {
      return this.intakes;
    }
    return this.intakes.filter(
      (choice) => choice.institutionId === null || choice.institutionId === institutionId
    );
  }

  static toChoices(years: AcademicYear[]): IntakeChoice[] {
    return years.flatMap((year) =>
      (year.intakes ?? []).map((intake) => ({
        id: intake.id,
        label: `${year.name} · ${intake.name}`,
        institutionId: year.institutionId ?? null,
      }))
    );
  }
}
