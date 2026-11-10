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
import { BuildingResponse } from '../../../../core/services/building.service';
import { ApplyOptionsService } from '../../../../core/services/apply-options.service';
import {
  ErrorHandlingService,
  ParsedError,
} from '../../../../core/services/error-handling.service';
import { Institution } from '../../../../core/models/institution.model';
import { StudentResponse } from '../../../../core/models/student.model';
import { contentOf } from '../../../../core/models/page-response.model';
import { describeSubmitted } from '../../../../core/utils/apply-wording';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ErrorDisplayComponent } from '../../../../shared/error-display/error-display.component';
import { BuildingGalleryComponent } from '../building-gallery/building-gallery.component';
import { CompanyCheckPanelComponent } from './company-check-panel/company-check-panel.component';
import { InstitutionPromptComponent } from './institution-prompt/institution-prompt.component';
import { CompanyCheck, groupByCompany, intakeIdsOf, intakeToKeep, isReady } from './company-checks';

/**
 * One form for up to 3 buildings from any companies (GH-495). Only buildings that can take the
 * student's institution are listed. Each company is checked as its buildings are picked, and on
 * submit the server creates one application per company, or none if any company refuses.
 */
@Component({
  selector: 'app-new-application',
  templateUrl: './new-application.component.html',
  styleUrls: ['./new-application.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatInputModule,
    MatFormFieldModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    ErrorDisplayComponent,
    BuildingGalleryComponent,
    CompanyCheckPanelComponent,
    InstitutionPromptComponent,
  ],
})
export class NewApplicationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly applicationService = inject(ApplicationService);
  private readonly studentService = inject(StudentService);
  private readonly institutionService = inject(InstitutionService);
  private readonly applyOptions = inject(ApplyOptionsService);
  private readonly errorService = inject(ErrorHandlingService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly applicationForm: FormGroup = this.fb.group({
    requiresAccessibility: [false],
    preferredBuildingIds: [[] as number[]],
    specialRequirements: [''],
    courseName: ['', Validators.required],
  });
  isSubmitting = false;
  profile: StudentResponse | null = null;
  studentId: number | null = null;
  studentInstitutionId: number | null = null;
  institutionName = '';
  /** Every building the student can apply to; {@link buildings} is what the search shows. */
  allBuildings: BuildingResponse[] = [];
  buildings: BuildingResponse[] = [];
  checks: CompanyCheck[] = [];
  submitError: ParsedError | null = null;
  loadError: ParsedError | null = null;
  isRetrying = false;

  ngOnInit(): void {
    if (!this.authService.currentUserValue) {
      this.router.navigate(['/login']);
      return;
    }
    this.loadStudentProfile();
  }

  get needsInstitution(): boolean {
    return !!this.profile && !this.studentInstitutionId;
  }

  get selectedIds(): number[] {
    return this.applicationForm.get('preferredBuildingIds')?.value ?? [];
  }

  /** Every picked company checked, with nothing in the way and an intake chosen. */
  get canSubmit(): boolean {
    return (
      this.applicationForm.valid &&
      !this.isSubmitting &&
      this.checks.length > 0 &&
      this.checks.every(isReady)
    );
  }

  loadStudentProfile(): void {
    this.loadError = null;
    this.isRetrying = true;
    this.studentService.getStudentProfile().subscribe({
      next: (profile) => {
        this.isRetrying = false;
        this.profile = profile;
        this.studentId = profile.id;
        this.studentInstitutionId = profile.institutionId || null;
        if (this.studentInstitutionId) {
          this.loadInstitutionName(this.studentInstitutionId);
          this.loadBuildings();
        }
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.isRetrying = false;
        this.loadError = this.errorService.parseError(error);
        if (this.loadError.statusCode === 404) {
          // /profile can only update a row; /profile-setup is the screen that creates one (#465).
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

  /** The institution was asked for inline and saved to the profile. */
  onInstitutionSaved(institution: Institution): void {
    this.studentInstitutionId = institution.id;
    this.institutionName = institution.name;
    this.loadBuildings();
    this.cdr.markForCheck();
  }

  loadBuildings(): void {
    this.applyOptions.getBuildings(this.studentInstitutionId).subscribe({
      next: (buildings) => {
        this.allBuildings = buildings;
        this.buildings = buildings;
        this.cdr.markForCheck();
      },
      error: () => {
        this.errorService.showWarning(
          'Could not load available buildings. Please refresh the page.'
        );
      },
    });
  }

  /** Searches the buildings the student can apply to, not every building. */
  onBuildingSearch(query: string): void {
    const q = query.trim().toLowerCase();
    this.buildings = !q
      ? this.allBuildings
      : this.allBuildings.filter((b) =>
          [b.name, b.address, b.companyName].some((field) => field?.toLowerCase().includes(q))
        );
    this.cdr.markForCheck();
  }

  onBuildingSelectionChanged(buildingIds: number[]): void {
    this.applicationForm.patchValue({ preferredBuildingIds: buildingIds });
    this.checks = groupByCompany(buildingIds, this.allBuildings, this.checks);
    this.checks.filter((c) => !c.options && !c.loading).forEach((c) => this.loadCheck(c.companyId));
    this.cdr.markForCheck();
  }

  onIntakeChosen(companyId: number, intakeId: number): void {
    this.updateCheck(companyId, { chosenIntakeId: intakeId });
  }

  /** Removes every building of the company from the picks. */
  removeCompany(companyId: number): void {
    const check = this.checks.find((c) => c.companyId === companyId);
    this.onBuildingSelectionChanged(
      this.selectedIds.filter((id) => !check?.buildingIds.includes(id))
    );
  }

  onSubmit(): void {
    this.submitError = null;
    if (!this.studentId) {
      // No id only means the load has not succeeded; a real 404 redirects from there (#465).
      this.errorService.showWarning('We could not load your profile yet. Please try again.');
      this.loadStudentProfile();
      return;
    }
    if (!this.canSubmit) {
      this.applicationForm.markAllAsTouched();
      this.errorService.showWarning(
        'Please fill in all required fields and resolve the notes on each company.'
      );
      return;
    }
    this.isSubmitting = true;
    const value = this.applicationForm.value;
    this.applicationService
      .createApplication({
        studentId: this.studentId,
        requiresAccessibility: value.requiresAccessibility || false,
        preferredBuildingIds: this.selectedIds,
        specialRequirements: value.specialRequirements || null,
        institutionName: this.institutionName || 'Not specified',
        courseName: value.courseName,
        intakeIds: intakeIdsOf(this.checks),
      })
      .subscribe({
        next: (created) => {
          this.isSubmitting = false;
          this.errorService.showSuccess(describeSubmitted(created));
          this.router.navigate(['/applications']);
        },
        error: (error) => {
          this.isSubmitting = false;
          // Shown inline; errorInterceptor has already opened the snackbar (#441). The form keeps
          // what was typed, and each company is checked again so a window that closed meanwhile
          // shows the next open intake or when the next window opens.
          this.submitError = this.errorService.parseError(error);
          this.checks.forEach((c) => this.loadCheck(c.companyId));
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

  private loadCheck(companyId: number): void {
    const check = this.checks.find((c) => c.companyId === companyId);
    if (!check) {
      return;
    }
    this.updateCheck(companyId, { loading: true, loadError: null });
    this.applyOptions.getOptions(check.buildingIds[0], this.studentInstitutionId).subscribe({
      next: (options) => {
        const current = this.checks.find((c) => c.companyId === companyId);
        this.updateCheck(companyId, {
          loading: false,
          options,
          companyName: options.companyName,
          chosenIntakeId: intakeToKeep(options, current?.chosenIntakeId ?? null),
        });
      },
      error: () =>
        this.updateCheck(companyId, {
          loading: false,
          loadError: `We could not check ${check.companyName}. Remove it or try again later.`,
        }),
    });
  }

  private updateCheck(companyId: number, changes: Partial<CompanyCheck>): void {
    this.checks = this.checks.map((c) => (c.companyId === companyId ? { ...c, ...changes } : c));
    this.cdr.markForCheck();
  }

  private loadInstitutionName(institutionId: number): void {
    this.institutionService.getInstitutions().subscribe({
      next: (response) => {
        const match = contentOf(response).find((i) => String(i.id) === String(institutionId));
        this.institutionName = match?.name ?? '';
        this.cdr.markForCheck();
      },
      error: () => this.errorService.showWarning('Could not load your institution details.'),
    });
  }
}
