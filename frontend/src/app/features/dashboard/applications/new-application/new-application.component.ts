import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ApplicationService } from '../../../../core/services/application.service';
import { StudentService } from '../../../../core/services/student.service';
import { InstitutionService } from '../../../../core/services/institution.service';
import { BuildingService, BuildingResponse } from '../../../../core/services/building.service';
import { AccommodationTypeService, AccommodationType } from '../../../../core/services/accommodation-type.service';
import { ErrorHandlingService, ParsedError } from '../../../../core/services/error-handling.service';

// Material imports
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { ErrorDisplayComponent } from '../../../../shared/error-display/error-display.component';

@Component({
  selector: 'app-new-application',
  templateUrl: './new-application.component.html',
  styleUrls: ['./new-application.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    ErrorDisplayComponent,
  ],
})
export class NewApplicationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly applicationService = inject(ApplicationService);
  private readonly studentService = inject(StudentService);
  private readonly institutionService = inject(InstitutionService);
  private readonly buildingService = inject(BuildingService);
  private readonly accommodationTypeService = inject(AccommodationTypeService);
  private readonly errorService = inject(ErrorHandlingService);
  private readonly cdr = inject(ChangeDetectorRef);

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

  accommodationTypes: AccommodationType[] = [];

  constructor() {
    this.applicationForm = this.fb.group({
      preferredAccommodationType: ['', Validators.required],
      preferredMoveInDate: ['', Validators.required],
      preferredDurationMonths: ['', [Validators.required, Validators.min(1), Validators.max(12)]],
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

    this.loadAccommodationTypes();
    this.loadStudentProfile();
  }

  loadBuildings(): void {
    // If student has an institution, only show buildings that serve that institution
    if (this.studentInstitutionId) {
      this.buildingService.getBuildingsByInstitution(this.studentInstitutionId).subscribe({
        next: (buildings) => {
          this.buildings = buildings;
          this.cdr.markForCheck();
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
        this.cdr.markForCheck();
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
              this.cdr.markForCheck();
            },
            error: (error) => {
              console.error('Error fetching institution:', error);
              this.loadBuildings();
              this.cdr.markForCheck();
            },
          });
        } else {
          this.loadBuildings();
        }
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.isRetrying = false;
        console.error('Error fetching student profile:', error);
        this.loadError = this.errorService.parseError(error);

        if (this.loadError.statusCode === 404) {
          this.errorService.showWarning('Please complete your profile first.');
          this.router.navigate(['/profile']);
        }
        this.cdr.markForCheck();
      },
    });
  }

  loadAccommodationTypes(): void {
    this.accommodationTypeService.getActiveTypes().subscribe({
      next: (types) => {
        this.accommodationTypes = types;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Failed to load accommodation types', error);
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
      this.router.navigate(['/profile']);
      return;
    }

    if (this.applicationForm.valid) {
      this.isSubmitting = true;

      const formValue = this.applicationForm.value;

      // Format the date to YYYY-MM-DD format for the backend
      const moveInDate = formValue.preferredMoveInDate;
      const formattedDate =
        moveInDate instanceof Date ? moveInDate.toISOString().split('T')[0] : moveInDate;

      // Ensure numeric fields are sent as numbers
      const durationMonths = formValue.preferredDurationMonths;
      const parsedDuration =
        typeof durationMonths === 'string' ? parseInt(durationMonths, 10) : durationMonths;

      const applicationData = {
        studentId: this.studentId,
        preferredAccommodationType: formValue.preferredAccommodationType,
        preferredMoveInDate: formattedDate,
        preferredDurationMonths: parsedDuration,
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
          this.router.navigate(['/applications']);
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.isSubmitting = false;
          console.error('Error submitting application:', error);
          this.submitError = this.errorService.parseError(error);

          // Don't show snackbar for validation errors - they'll see the inline error
          if (this.submitError.type !== 'validation') {
            this.errorService.showErrorSnackbar(this.submitError);
          }
          this.cdr.markForCheck();
        },
      });
    } else {
      this.applicationForm.markAllAsTouched();
      this.errorService.showWarning('Please fill in all required fields correctly');
    }
  }

  dismissError(): void {
    this.submitError = null;
  }

  retrySubmit(): void {
    this.onSubmit();
  }
}
