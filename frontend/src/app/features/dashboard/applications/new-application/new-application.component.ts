import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ApplicationService } from '../../../../core/services/application.service';
import { StudentService } from '../../../../core/services/student.service';
import { InstitutionService } from '../../../../core/services/institution.service';
import { BuildingService, BuildingResponse } from '../../../../core/services/building.service';
import {
  ErrorHandlingService,
  ParsedError,
} from '../../../../core/services/error-handling.service';

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
import { BuildingGalleryComponent } from '../building-gallery/building-gallery.component';
import { User } from '../../../../core/models/user.model';
import { contentOf } from '../../../../core/models/page-response.model';

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
    BuildingGalleryComponent,
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
  private readonly errorService = inject(ErrorHandlingService);
  private readonly cdr = inject(ChangeDetectorRef);

  applicationForm: FormGroup;
  isSubmitting = false;
  currentUser: User | null = null;
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
      preferredBuildingIds: [[] as number[]],
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
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Failed to load buildings for institution', error);
          this.errorService.showWarning(
            'Could not filter buildings by your institution. Showing all buildings instead.'
          );
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
        this.errorService.showWarning(
          'Could not load available buildings. Please refresh the page.'
        );
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
            next: (response) => {
              const institutions = contentOf(response);
              const institution = institutions.find(
                (inst) => String(inst.id) === String(profile.institutionId)
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
              this.errorService.showWarning(
                'Could not load your institution details. Building suggestions may be less relevant.'
              );
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
          // /profile can only update a row; /profile-setup is the screen that creates one (#465).
          // replaceUrl, so the back button does not land here and bounce straight forward again.
          this.errorService.showWarning('Please complete your profile first.');
          this.router.navigate(['/profile-setup'], { replaceUrl: true });
        }
        this.cdr.markForCheck();
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
      // No id only means the load has not succeeded. A 404 has already gone to /profile-setup, so
      // this student may well have a profile, and student.user_id has no unique constraint to
      // refuse a second row. Ask again: a real 404 redirects from there (#465).
      this.errorService.showWarning('We could not load your profile yet. Please try again.');
      this.loadStudentProfile();
      return;
    }

    if (this.applicationForm.valid) {
      this.isSubmitting = true;

      const formValue = this.applicationForm.value;

      const applicationData = {
        studentId: this.studentId,
        requiresAccessibility: formValue.requiresAccessibility || false,
        preferredBuildingIds: (formValue.preferredBuildingIds as number[]) || [],
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

  onBuildingSearch(query: string): void {
    if (!query) {
      this.loadBuildings();
      return;
    }
    this.buildingService.searchBuildings({ name: query, size: 10 }).subscribe({
      next: (response) => {
        this.buildings = response.content;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Building search failed', error);
        this.errorService.showWarning('Building search failed. Please try again.');
      },
    });
  }

  onBuildingSelectionChanged(buildingIds: number[]): void {
    this.applicationForm.patchValue({ preferredBuildingIds: buildingIds });
    this.cdr.markForCheck();
  }

  dismissError(): void {
    this.submitError = null;
  }

  retrySubmit(): void {
    this.onSubmit();
  }
}
