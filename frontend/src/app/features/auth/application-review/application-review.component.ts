import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../../core/models/user.model';
import { StudentService } from '../../../core/services/student.service';
import { StudentRequest, StudentResponse } from '../../../core/models/student.model';
import { YearOfStudy } from '../../../core/models/year-of-study.model';
import { InstitutionService } from '../../../core/services/institution.service';
import { YearOfStudyService } from '../../../core/services/year-of-study.service';
import { Institution } from '../../../core/models/institution.model';
import { ApplicationService } from '../../../core/services/application.service';
import {
  ApplicationDraftService,
  ApplicationDraft,
} from '../../../core/services/application-draft.service';
import { ErrorHandlingService, ParsedError } from '../../../core/services/error-handling.service';
import { ErrorDisplayComponent } from '../../../shared/error-display/error-display.component';
import { contentOf } from '../../../core/models/page-response.model';

@Component({
  selector: 'app-application-review',
  templateUrl: './application-review.component.html',
  styleUrls: ['./application-review.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatChipsModule,
    ErrorDisplayComponent,
  ],
})
export class ApplicationReviewComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly studentService = inject(StudentService);
  private readonly yearOfStudyService = inject(YearOfStudyService);
  private readonly institutionService = inject(InstitutionService);
  private readonly applicationService = inject(ApplicationService);
  private readonly draftService = inject(ApplicationDraftService);
  private readonly errorService = inject(ErrorHandlingService);
  private readonly cdr = inject(ChangeDetectorRef);

  isSubmitting = false;
  termsAccepted = false;
  currentUser: User | null = null;
  studentProfile: StudentResponse | null = null;
  yearOfStudy: readonly YearOfStudy[] = [];
  institutions: readonly Institution[] = [];
  draft: ApplicationDraft | null = null;
  submitError: ParsedError | null = null;

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
      return;
    }

    // Load draft — if none, redirect back to application form
    this.draft = this.draftService.getDraft();
    if (!this.draft) {
      this.errorService.showWarning(
        'No application data found. Please fill in the application form first.'
      );
      this.router.navigate(['/application-submission']);
      return;
    }

    this.studentService.getStudentProfile().subscribe({
      next: (student) => {
        this.studentProfile = student;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to load student profile', err);
        this.errorService.showWarning('Could not load your profile. Please try again.');
        this.router.navigate(['/application-submission']);
      },
    });

    this.yearOfStudyService.getYearsOfStudy().subscribe({
      next: (yearOfStudy) => {
        this.yearOfStudy = contentOf(yearOfStudy);
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to load year of study', err);
        this.errorService.showWarning('Could not load year of study data.');
      },
    });

    this.institutionService.getInstitutions().subscribe({
      next: (institutions) => {
        this.institutions = contentOf(institutions);
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to load institutions', err);
        this.errorService.showWarning('Could not load institution data.');
      },
    });
  }

  onSubmit() {
    if (!this.draft || !this.termsAccepted) return;

    const institutionId = this.studentProfile?.institutionId;
    const yearOfStudyId = this.studentProfile?.yearOfStudyId;
    if (institutionId == null || yearOfStudyId == null) {
      this.errorService.showWarning(
        'Your profile is missing institution details. Please complete profile setup first.'
      );
      return;
    }

    this.isSubmitting = true;
    this.submitError = null;

    // Step 1: Update student profile with deferred fields
    const dob = this.draft.dateOfBirth;
    const profileUpdate: StudentRequest = {
      firstName: this.currentUser!.firstName,
      lastName: this.currentUser!.lastName,
      email: this.currentUser!.email,
      studentNumber: this.draft.studentNumber,
      phoneNumber: this.draft.phoneNumber,
      dateOfBirth: dob instanceof Date ? dob.toISOString() : dob,
      gender: this.draft.gender,
      specialRequirements: this.draft.specialRequirements ?? undefined,
      institutionId,
      yearOfStudyId,
    };

    this.studentService.updateStudentProfile(profileUpdate).subscribe({
      next: () => {
        // Step 2: Submit the application
        this.submitApplication();
      },
      error: (error) => {
        this.isSubmitting = false;
        console.error('Error updating profile:', error);
        this.submitError = this.errorService.parseError(error);
        this.errorService.showErrorSnackbar(this.submitError);
        this.cdr.markForCheck();
      },
    });
  }

  private submitApplication(): void {
    const applicationData = {
      studentId: this.draft!.studentId,
      requiresAccessibility: this.draft!.requiresAccessibility,
      preferredBuildingIds: this.draft!.preferredBuildingIds?.length
        ? this.draft!.preferredBuildingIds
        : null,
      specialRequirements: this.draft!.specialRequirements || null,
      institutionName: this.draft!.institutionName,
      courseName: this.draft!.courseName,
    };

    this.applicationService.createApplication(applicationData).subscribe({
      next: (created) => {
        this.isSubmitting = false;
        this.draftService.clearDraft();
        this.errorService.showSuccess('Application submitted successfully!');
        this.router.navigate(['/application-confirmation', created.id]);
      },
      error: (error) => {
        this.isSubmitting = false;
        console.error('Error submitting application:', error);
        this.submitError = this.errorService.parseError(error);
        if (this.submitError.type !== 'validation') {
          this.errorService.showErrorSnackbar(this.submitError);
        }
        this.cdr.markForCheck();
      },
    });
  }

  dismissError(): void {
    this.submitError = null;
  }

  retrySubmit(): void {
    this.onSubmit();
  }

  getInstitutionName(): string {
    if (!this.institutions || !this.studentProfile?.institutionId) {
      return 'N/A';
    }
    const institutionId = String(this.studentProfile.institutionId);
    const inst = this.institutions.find((i) => String(i.id) === institutionId);
    return inst?.name || 'N/A';
  }

  getYearOfStudyName(): string {
    if (!this.yearOfStudy || !this.studentProfile?.yearOfStudyId) {
      return 'N/A';
    }
    const yearOfStudyId = String(this.studentProfile.yearOfStudyId);
    const year = this.yearOfStudy.find((y) => String(y.id) === yearOfStudyId);
    return year?.yearOfStudy || 'N/A';
  }
}
