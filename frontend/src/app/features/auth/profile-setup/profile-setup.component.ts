import { Component, OnInit, ChangeDetectionStrategy, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { InstitutionService } from '../../../core/services/institution.service';
import { YearOfStudyService } from '../../../core/services/year-of-study.service';
import { StudentService } from '../../../core/services/student.service';
import { ErrorHandlingService } from '../../../core/services/error-handling.service';
import { Institution } from '../../../core/models/institution.model';
import { YearOfStudy } from '../../../core/models/year-of-study.model';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatStepperModule } from '@angular/material/stepper';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-profile-setup',
  templateUrl: './profile-setup.component.html',
  styleUrls: ['./profile-setup.component.css'],
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatStepperModule,
    MatIconModule,
    MatDividerModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileSetupComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly institutionService = inject(InstitutionService);
  private readonly yearOfStudyService = inject(YearOfStudyService);
  private readonly studentService = inject(StudentService);
  private readonly errorService = inject(ErrorHandlingService);

  profileForm: FormGroup;
  isSubmitting = false;
  currentUser: any;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  institutions: Institution[] = [];
  yearsOfStudy: YearOfStudy[] = [];

  constructor() {
    this.profileForm = this.fb.group({
      institution: ['', [Validators.required]],
      yearOfStudy: ['', [Validators.required]],
      specialRequirements: [''],
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
    }

    this.institutionService.getInstitutions().subscribe({
      next: (institutions) => {
        console.log('Institutions API response:', institutions);
        if (institutions && Array.isArray((institutions as any).content)) {
          this.institutions = (institutions as any).content;
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } else if (Array.isArray(institutions)) {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          this.institutions = institutions;
        } else {
          this.institutions = [];
        }
      },
      error: (error) => {
        console.error('Error fetching institutions:', error);
        this.errorService.showWarning('Could not load institutions. Please refresh the page.');
        this.institutions = [];
      },
    });

    this.yearOfStudyService.getYearsOfStudy().subscribe({
      next: (yearOfStudy) => {
        console.log('YearsOfStudy API response:', yearOfStudy);
        if (yearOfStudy && Array.isArray((yearOfStudy as any).content)) {
          this.yearsOfStudy = (yearOfStudy as any).content;
        } else if (Array.isArray(yearOfStudy)) {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          this.yearsOfStudy = yearOfStudy;
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } else {
          this.yearsOfStudy = [];
        }
      },
      error: (error) => {
        console.error('Error fetching years of study:', error);
        this.errorService.showWarning('Could not load years of study. Please refresh the page.');
        this.yearsOfStudy = [];
      },
    });
  }

  onSubmit() {
    if (this.profileForm.valid) {
      this.isSubmitting = true;

      const studentData = {
        firstName: this.currentUser.firstName,
        lastName: this.currentUser.lastName,
        email: this.currentUser.email,
        institutionId: this.profileForm.get('institution')?.value,
        yearOfStudyId: this.profileForm.get('yearOfStudy')?.value,
        specialRequirements: this.profileForm.get('specialRequirements')?.value,
        userId: this.currentUser.id,
      };

      this.studentService.createStudentProfile(studentData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.router.navigate(['/application-submission']);
        },
        error: (error) => {
          this.isSubmitting = false;
          console.error('Error updating profile:', error);
          this.errorService.showErrorSnackbar(this.errorService.parseError(error));
        },
      });
    } else {
      this.profileForm.markAllAsTouched();
    }
  }
}
