import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject,
} from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { Institution } from '../../../../../core/models/institution.model';
import { StudentResponse } from '../../../../../core/models/student.model';
import { InstitutionService } from '../../../../../core/services/institution.service';
import { StudentService } from '../../../../../core/services/student.service';
import { contentOf } from '../../../../../core/models/page-response.model';

/**
 * Asks once for the institution of a student whose profile has none, and saves it to the profile
 * through the existing profile update (GH-495). It decides which buildings and intakes they see.
 */
@Component({
  selector: 'app-institution-prompt',
  templateUrl: './institution-prompt.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
  ],
})
export class InstitutionPromptComponent implements OnInit {
  private readonly institutionService = inject(InstitutionService);
  private readonly studentService = inject(StudentService);
  private readonly cdr = inject(ChangeDetectorRef);

  @Input({ required: true }) profile!: StudentResponse;
  @Output() saved = new EventEmitter<Institution>();

  readonly institution = new FormControl<number | null>(null, Validators.required);
  institutions: Institution[] = [];
  isSaving = false;
  error: string | null = null;

  ngOnInit(): void {
    this.institutionService.getInstitutions().subscribe({
      next: (response) => {
        this.institutions = contentOf(response);
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'We could not load the institutions. Please refresh the page.';
        this.cdr.markForCheck();
      },
    });
  }

  save(): void {
    const id = this.institution.value;
    const chosen = this.institutions.find((i) => i.id === id);
    if (!chosen) {
      this.institution.markAsTouched();
      return;
    }
    this.isSaving = true;
    this.error = null;
    const p = this.profile;
    this.studentService
      .updateStudentProfile({
        studentNumber: p.studentNumber,
        phoneNumber: p.phoneNumber,
        dateOfBirth: p.dateOfBirth,
        institutionId: chosen.id,
        yearOfStudyId: p.yearOfStudyId,
        gender: p.gender,
        specialRequirements: p.specialRequirements,
        emergencyContactName: p.emergencyContactName,
        emergencyContactRelationship: p.emergencyContactRelationship,
        emergencyContactPhone: p.emergencyContactPhone,
        emergencyContactEmail: p.emergencyContactEmail,
        firstName: p.firstName,
        lastName: p.lastName,
        email: p.email,
      })
      .subscribe({
        next: () => {
          this.isSaving = false;
          this.saved.emit(chosen);
          this.cdr.markForCheck();
        },
        error: () => {
          this.isSaving = false;
          this.error =
            'Your institution was not saved. Please try again, or add it on your profile.';
          this.cdr.markForCheck();
        },
      });
  }
}
