import { Component, OnInit, ChangeDetectionStrategy, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ApplicationService } from '../../../core/services/application.service';
import { StudentService } from '../../../core/services/student.service';
import { InstitutionService } from '../../../core/services/institution.service';
import { BuildingService, BuildingResponse } from '../../../core/services/building.service';
import { ErrorHandlingService, ParsedError } from '../../../core/services/error-handling.service';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ErrorDisplayComponent } from '../../../shared/error-display/error-display.component';
import { BuildingGalleryComponent } from '../../dashboard/applications/building-gallery/building-gallery.component';

@Component({
  selector: 'app-application-submission',
  templateUrl: './application-submission.component.html',
  styleUrls: ['./application-submission.component.css'],
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatIconModule,
    MatCheckboxModule,
    ErrorDisplayComponent,
    BuildingGalleryComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationSubmissionComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly applicationService = inject(ApplicationService);
  private readonly studentService = inject(StudentService);
  private readonly institutionService = inject(InstitutionService);
  private readonly buildingService = inject(BuildingService);
  private readonly errorService = inject(ErrorHandlingService);

  applicationForm: FormGroup;
  isSubmitting = false;
  currentUser: any;
  studentId: number | null = null;
  studentInstitutionId: number | null = null;
  institutionName = '';
  buildings: BuildingResponse[] = [];
  submitError: ParsedError | null = null;
  loadError: ParsedError | null = null;
  isRetrying = false;

  constructor() {
    this.applicationForm = this.fb.group({
      requiresAccessibility: [false],
      preferredBuildingId: [''],
      specialRequirements: [''],
      courseName: ['', Validators.required],
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
      return;
    }

    this.loadStudentProfile();
  }

  loadBuildings(): void {
    // If student has an institution, only show buildings that serve that institution
    if (this.studentInstitutionId) {
      this.buildingService.getBuildingsByInstitution(this.studentInstitutionId).subscribe({
        next: (buildings) => {
          this.buildings = buildings;
        },
        error: (error) => {
          console.error('Failed to load buildings for institution', error);
          // Fall back to all buildings
          this.loadAllBuildings();
        },
      });
    } else {
      this.loadAllBuildings();
    }
  }

  private loadAllBuildings(): void {
    this.buildingService.getAllBuildingsNoPagination().subscribe({
      next: (buildings) => {
        this.buildings = buildings;
      },
      error: (error) => {
        console.error('Failed to load buildings', error);
      },
    });
  }

  loadStudentProfile(): void {
    this.loadError = null;
    this.isRetrying = true;

    this.studentService.getStudentProfile().subscribe({
      next: (profile) => {
        this.isRetrying = false;
        this.studentId = profile.id;
        this.studentInstitutionId = profile.institutionId || null;

        // Get institution name and load buildings after we know the institution
        if (profile.institutionId) {
          this.institutionService.getInstitutions().subscribe({
            next: (response: any) => {
              const institutions = Array.isArray((response as any).content)
                ? (response as any).content
                : Array.isArray(response)
                ? response
                : [];

              const institution = institutions.find(
                (inst: any) => String(inst.id) === String(profile.institutionId)
              );

              if (institution) {
                this.institutionName = institution.name;
              }
              // Load buildings after we know the institution
              this.loadBuildings();
            },
            error: (error) => {
              console.error('Error fetching institution:', error);
              this.loadBuildings();
            },
          });
        } else {
          this.loadBuildings();
        }
      },
      error: (error) => {
        this.isRetrying = false;
        console.error('Error fetching student profile:', error);
        this.loadError = this.errorService.parseError(error);

        if (this.loadError.statusCode === 404) {
          this.errorService.showWarning('Please complete your profile first.');
          this.router.navigate(['/profile-setup']);
        }
      },
    });
  }

  retryLoadProfile(): void {
    this.loadStudentProfile();
  }

  onSubmit() {
    // Clear previous errors
    this.submitError = null;

    if (!this.studentId) {
      this.errorService.showWarning('Student profile not found. Please complete your profile first.');
      this.router.navigate(['/profile-setup']);
      return;
    }

    if (this.applicationForm.valid) {
      this.isSubmitting = true;

      const formValue = this.applicationForm.value;

      const applicationData = {
        studentId: this.studentId,
        requiresAccessibility: formValue.requiresAccessibility || false,
        preferredBuildingId: formValue.preferredBuildingId || null,
        specialRequirements: formValue.specialRequirements || null,
        institutionName: this.institutionName || 'Not specified',
        courseName: formValue.courseName,
      };

      this.applicationService.createApplication(applicationData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.errorService.showSuccess('Application submitted successfully!');
          this.router.navigate(['/document-upload']);
        },
        error: (error) => {
          this.isSubmitting = false;
          console.error('Error submitting application:', error);
          this.submitError = this.errorService.parseError(error);

          // Don't show snackbar for validation errors - they'll see the inline error
          if (this.submitError.type !== 'validation') {
            this.errorService.showErrorSnackbar(this.submitError);
          }
        },
      });
    } else {
      this.applicationForm.markAllAsTouched();
      this.errorService.showWarning('Please fill in all required fields correctly');
    }
  }

  onBuildingSelected(buildingId: number | null): void {
    this.applicationForm.patchValue({ preferredBuildingId: buildingId });
  }

  dismissError(): void {
    this.submitError = null;
  }

  retrySubmit(): void {
    this.onSubmit();
  }

  /**
   * Skip application is no longer allowed since documents must be linked to an application.
   * This method shows a warning to the user.
   */
  skipApplication() {
    this.errorService.showWarning('An application is required before you can upload documents. Please complete your application.');
  }
}
