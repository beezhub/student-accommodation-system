import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { ApplicationService } from '../../../core/services/application.service';
import { StudentService } from '../../../core/services/student.service';
import { InstitutionService } from '../../../core/services/institution.service';
import { BuildingService, BuildingResponse } from '../../../core/services/building.service';
import { DocumentService } from '../../../core/services/document.service';
import { DocumentTypeService } from '../../../core/services/document-type.service';
import { ErrorHandlingService, ParsedError } from '../../../core/services/error-handling.service';
import { CommonModule } from '@angular/common';
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
    CommonModule,
    RouterModule,
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
  private readonly documentService = inject(DocumentService);
  private readonly documentTypeService = inject(DocumentTypeService);
  private readonly errorService = inject(ErrorHandlingService);
  private readonly cdr = inject(ChangeDetectorRef);

  applicationForm: FormGroup;
  isSubmitting = false;
  currentUser: any;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  studentId: number | null = null;
  studentInstitutionId: number | null = null;
  institutionName = '';
  buildings: BuildingResponse[] = [];
  submitError: ParsedError | null = null;
  loadError: ParsedError | null = null;
  isRetrying = false;

  /** Document readiness checklist */
  requiredDocTypes: { id: number; name: string }[] = [];
  /** Uploaded document type names (lowercase) — used for name-based matching */
  uploadedDocTypeNames: Set<string> = new Set();
  /** Derived from the first selected building — used to fetch tenant-specific required types */
  selectedTenantId: number | null = null;
  isLoadingDocReadiness = false;

  get missingRequiredDocs(): { id: number; name: string }[] {
    return this.requiredDocTypes.filter(
      dt => !this.uploadedDocTypeNames.has(dt.name.toLowerCase())
    );
  }

  get hasAllRequiredDocs(): boolean {
    return this.missingRequiredDocs.length === 0;
  }

  constructor() {
    this.applicationForm = this.fb.group({
      requiresAccessibility: [false],
      preferredBuildingIds: [[]],
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

    this.applicationService.hasActiveApplication().subscribe((hasActive) => {
      if (hasActive) {
        this.errorService.showWarning('You already have an active application. You can track it in your dashboard.');
        this.router.navigate(['/dashboard']);
        return;
      }
      this.loadStudentProfile();
    });

    this.loadDocumentReadiness();
  }

  private loadDocumentReadiness(tenantId?: number): void {
    this.isLoadingDocReadiness = true;
    forkJoin({
      requiredTypes: this.documentTypeService.getRequiredDocumentTypes(tenantId),
      uploadedDocs: this.documentService.getUploadedDocuments(),
    }).subscribe({
      next: ({ requiredTypes, uploadedDocs }) => {
        this.requiredDocTypes = (requiredTypes as any[]).map((t: any) => ({
          id: Number(t.id),
          name: t.name ?? t.typeName,
        }));
        // Name-based matching: compare type names case-insensitively.
        // Student's uploaded docs reference platform type names; tenant shadows
        // may have different IDs but the same name — so ID-based matching would fail.
        const anyDocs = uploadedDocs as any[];
        this.uploadedDocTypeNames = new Set(
          anyDocs.map((d: any) => String(d.typeName ?? d.documentTypeName ?? d.name ?? '').toLowerCase())
        );
        this.isLoadingDocReadiness = false;
        this.cdr.markForCheck();
      },
      error: () => {
        // Non-blocking: if we can't load, show checklist as unchecked
        this.isLoadingDocReadiness = false;
        this.cdr.markForCheck();
      },
    });
  }

  private readonly BUILDINGS_PAGE_SIZE = 5;

  loadBuildings(): void {
    if (this.studentInstitutionId) {
      this.buildingService.getBuildingsByInstitution(this.studentInstitutionId).subscribe({
        next: (buildings) => {
          this.buildings = buildings.slice(0, this.BUILDINGS_PAGE_SIZE);
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Failed to load buildings for institution', error);
          this.loadAllBuildings();
        },
      });
    } else {
      this.loadAllBuildings();
    }
  }

  private loadAllBuildings(): void {
    this.buildingService.getAllBuildings(0, this.BUILDINGS_PAGE_SIZE).subscribe({
      next: (response) => {
        this.buildings = response.content;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Failed to load buildings', error);
      },
    });
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
                ? // eslint-disable-next-line @typescript-eslint/no-explicit-any
                  response
                : // eslint-disable-next-line @typescript-eslint/no-explicit-any
                  [];

              // eslint-disable-next-line @typescript-eslint/no-explicit-any

              const institution = institutions.find(
                (inst: any) => String(inst.id) === String(profile.institutionId)
              );

              if (institution) {
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
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
      this.errorService.showWarning(
        'Student profile not found. Please complete your profile first.'
      );
      this.router.navigate(['/profile-setup']);
      return;
    }

    if (this.applicationForm.valid) {
      this.isSubmitting = true;

      const formValue = this.applicationForm.value;

      const applicationData = {
        studentId: this.studentId,
        requiresAccessibility: formValue.requiresAccessibility || false,
        preferredBuildingIds: formValue.preferredBuildingIds?.length ? formValue.preferredBuildingIds : null,
        specialRequirements: formValue.specialRequirements || null,
        institutionName: this.institutionName || 'Not specified',
        courseName: formValue.courseName,
      };

      this.applicationService.createApplication(applicationData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.errorService.showSuccess('Application submitted successfully!');
          this.router.navigate(['/dashboard']);
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

  onBuildingSelectionChanged(buildingIds: number[]): void {
    this.applicationForm.patchValue({ preferredBuildingIds: buildingIds });

    // Derive the target tenant from the first selected building and reload
    // document readiness with that tenant's effective required types.
    if (buildingIds.length > 0) {
      const firstBuilding = this.buildings.find(b => b.id === buildingIds[0]);
      const tenantId = firstBuilding?.tenantId ?? null;
      if (tenantId !== this.selectedTenantId) {
        this.selectedTenantId = tenantId;
        this.loadDocumentReadiness(tenantId ?? undefined);
      }
    } else {
      this.selectedTenantId = null;
      this.loadDocumentReadiness();
    }
  }

  dismissError(): void {
    this.submitError = null;
  }

  retrySubmit(): void {
    this.onSubmit();
  }

}
