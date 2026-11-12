import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  DestroyRef,
  inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
import { parseLocalDate } from '../../../core/utils/local-date';
import { ApplicationWizardService } from '../../../core/services/application-wizard.service';
import { ApplicationStep4AnswersService } from '../../../core/services/application-step4-answers.service';
import { describeSubmitted } from '../../../core/utils/apply-wording';
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
import { User } from '../../../core/models/user.model';
import { contentOf } from '../../../core/models/page-response.model';
import { StudentResponse } from '../../../core/models/student.model';
import { GENDER_OPTIONS, normalizeGender } from '../../../core/utils/gender';

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
  private readonly wizardService = inject(ApplicationWizardService);
  private readonly answersService = inject(ApplicationStep4AnswersService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private keepingAnswers = false;

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

  /** Document readiness checklist */
  requiredDocTypes: { id: number; name: string }[] = [];
  /** Uploaded document type names (lowercase) — used for name-based matching */
  uploadedDocTypeNames: Set<string> = new Set();
  /** Derived from the first selected building — used to fetch tenant-specific required types */
  selectedTenantId: number | null = null;
  isLoadingDocReadiness = false;

  get missingRequiredDocs(): { id: number; name: string }[] {
    return this.requiredDocTypes.filter(
      (dt) => !this.uploadedDocTypeNames.has(dt.name.toLowerCase())
    );
  }

  get hasAllRequiredDocs(): boolean {
    return this.missingRequiredDocs.length === 0;
  }

  /** Student profile data — used for pre-populating deferred fields */
  studentProfile: StudentResponse | null = null;

  readonly genderOptions = GENDER_OPTIONS;

  constructor() {
    this.applicationForm = this.fb.group({
      // Deferred profile fields
      studentNumber: ['', Validators.required],
      phoneNumber: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      dateOfBirth: ['', Validators.required],
      gender: ['', Validators.required],
      // Application fields
      requiresAccessibility: [false],
      preferredBuildingIds: [[]],
      specialRequirements: [''],
      courseName: ['', Validators.required],
      // On this step rather than the review page, because Continue to Review submits (#538)
      termsAccepted: [false, Validators.requiredTrue],
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
        this.errorService.showWarning(
          'You already have an active application. You can track it in your dashboard.'
        );
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
        this.requiredDocTypes = requiredTypes.map((t) => ({
          id: Number(t.id),
          name: t.name,
        }));
        // Name-based matching: compare type names case-insensitively.
        // Student's uploaded docs reference platform type names; tenant shadows
        // may have different IDs but the same name — so ID-based matching would fail.
        // The two endpoints name the same field differently, so both spellings are read. Typed
        // rather than cast: the fallback chain is the uncertainty, not the whole object (GH-158).
        this.uploadedDocTypeNames = new Set(
          uploadedDocs.map((d) => {
            const named = d as { typeName?: string; documentTypeName?: string; name?: string };
            return String(
              named.typeName ?? named.documentTypeName ?? named.name ?? ''
            ).toLowerCase();
          })
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
          if (buildings && buildings.length > 0) {
            this.buildings = buildings.slice(0, this.BUILDINGS_PAGE_SIZE);
            this.cdr.markForCheck();
          } else {
            this.loadAllBuildings();
          }
        },
        error: (error) => {
          console.error('Failed to load buildings for institution', error);
          this.errorService.showWarning(
            'Could not filter buildings by your institution. Showing all buildings instead.'
          );
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
        this.buildings = response?.content ?? [];
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

  onBuildingSearch(query: string): void {
    if (!query) {
      this.loadBuildings();
      return;
    }
    this.buildingService.searchBuildings({ name: query, size: 10 }).subscribe({
      next: (response) => {
        this.buildings = response?.content ?? [];
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Building search failed', error);
        this.errorService.showWarning('Building search failed. Please try again.');
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
        this.studentProfile = profile;
        // Read before the profile is patched in: patching starts keeping answers, and would
        // otherwise overwrite what the student typed before a refresh (#550).
        const kept = this.currentUser ? this.answersService.load(this.currentUser.id) : null;

        // Pre-populate deferred profile fields if the student already has them
        if (profile.studentNumber) {
          this.applicationForm.patchValue({ studentNumber: profile.studentNumber });
        }
        if (profile.phoneNumber) {
          this.applicationForm.patchValue({ phoneNumber: profile.phoneNumber });
        }
        if (profile.dateOfBirth) {
          this.applicationForm.patchValue({ dateOfBirth: parseLocalDate(profile.dateOfBirth) });
        }
        if (profile.gender) {
          this.applicationForm.patchValue({ gender: normalizeGender(profile.gender) });
        }
        if (profile.specialRequirements) {
          this.applicationForm.patchValue({ specialRequirements: profile.specialRequirements });
        }
        // What the student typed before a refresh wins over the saved profile.
        if (kept) {
          this.applicationForm.patchValue(kept);
        }
        this.keepAnswers();

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
                this.cdr.markForCheck();
              }
              // Load buildings after we know the institution
              this.loadBuildings();
            },
            error: (error) => {
              console.error('Error fetching institution:', error);
              this.errorService.showWarning(
                'Could not load your institution details. Building suggestions may be less relevant.'
              );
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
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * Keeps step 4's answers in this tab until the application is created, so a refresh does not
   * empty the form (#550). The confirmation is left out: the student confirms again.
   */
  private keepAnswers(): void {
    const userId = this.currentUser?.id;
    if (this.keepingAnswers || userId == null) {
      return;
    }
    this.keepingAnswers = true;
    this.applicationForm.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      const v = this.applicationForm.getRawValue();
      this.answersService.save(userId, {
        studentNumber: v.studentNumber ?? '',
        phoneNumber: v.phoneNumber ?? '',
        dateOfBirth: v.dateOfBirth instanceof Date ? v.dateOfBirth : null,
        gender: v.gender ?? '',
        courseName: v.courseName ?? '',
        requiresAccessibility: !!v.requiresAccessibility,
        preferredBuildingIds: v.preferredBuildingIds ?? [],
        specialRequirements: v.specialRequirements ?? '',
      });
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

    // GH-153: the backend refuses an application from an unverified address with a 403. Caught
    // here so the student is sent somewhere they can act, rather than filling in the review step
    // and being stopped by a snackbar at the end of it. The backend gate is the real enforcement;
    // this only saves the wasted trip.
    if (this.currentUser && this.currentUser.emailVerified !== true) {
      this.errorService.showWarning('Confirm your email address before submitting an application.');
      this.router.navigate(['/email-verification-required']);
      return;
    }

    if (!this.applicationForm.valid) {
      this.applicationForm.markAllAsTouched();
      this.errorService.showWarning('Please fill in all required fields correctly');
      return;
    }

    const user = this.currentUser;
    const institutionId = this.studentProfile?.institutionId;
    const yearOfStudyId = this.studentProfile?.yearOfStudyId;
    if (!user || institutionId == null || yearOfStudyId == null) {
      this.errorService.showWarning(
        'Your profile is missing institution details. Please complete profile setup first.'
      );
      return;
    }

    // #538: save the profile and create the application here, so nothing the student entered
    // lives only in this browser. The review page shows what the server saved.
    const formValue = this.applicationForm.value;
    this.isSubmitting = true;
    this.wizardService
      .submit(user, {
        studentId: this.studentId,
        institutionId,
        yearOfStudyId,
        institutionName: this.institutionName || 'Not specified',
        studentNumber: formValue.studentNumber,
        phoneNumber: formValue.phoneNumber,
        dateOfBirth: formValue.dateOfBirth,
        gender: formValue.gender,
        courseName: formValue.courseName,
        requiresAccessibility: formValue.requiresAccessibility || false,
        preferredBuildingIds: formValue.preferredBuildingIds ?? [],
        specialRequirements: formValue.specialRequirements || null,
      })
      .subscribe({
        next: (created) => {
          this.isSubmitting = false;
          this.answersService.clear(user.id);
          this.errorService.showSuccess(describeSubmitted(created));
          this.router.navigate(['/application-review'], {
            queryParams: { ids: created.map((a) => a.id).join(',') },
          });
        },
        error: (error) => {
          this.isSubmitting = false;
          this.submitError = this.errorService.parseError(error);
          if (this.submitError.type !== 'validation') {
            this.errorService.showErrorSnackbar(this.submitError);
          }
          this.cdr.markForCheck();
        },
      });
  }

  onBuildingSelectionChanged(buildingIds: number[]): void {
    this.applicationForm.patchValue({ preferredBuildingIds: buildingIds });

    // Derive the target tenant from the first selected building and reload
    // document readiness with that tenant's effective required types.
    if (buildingIds.length > 0) {
      const firstBuilding = this.buildings.find((b) => b.id === buildingIds[0]);
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
