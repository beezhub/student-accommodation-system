import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  inject,
} from '@angular/core';

import { RouterModule, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { YearOfStudyService } from '../../../core/services/year-of-study.service';
import { InstitutionService } from '../../../core/services/institution.service';
import { Institution } from '../../../core/models/institution.model';
import { YearOfStudy } from '../../../core/models/year-of-study.model';
import { StudentService } from '../../../core/services/student.service';
import { StudentProfile } from '../../../core/models/student-profile';
import { MaterialModule } from '../../../shared/material.module';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { StudentRequest } from '../../../core/models/student.model';
import { ChangePasswordDialogComponent } from './change-password-dialog/change-password-dialog.component';
import { DeleteAccountDialogComponent } from './delete-account-dialog/delete-account-dialog.component';
import { ErrorHandlingService, ParsedError } from '../../../core/services/error-handling.service';
import { parseLocalDate, toLocalDateString } from '../../../core/utils/local-date';
import { GENDER_OPTIONS, normalizeGender } from '../../../core/utils/gender';
import { ErrorDisplayComponent } from '../../../shared/error-display/error-display.component';
import { User } from '../../../core/models/user.model';
import { contentOf } from '../../../core/models/page-response.model';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterModule, ReactiveFormsModule, MaterialModule, ErrorDisplayComponent],
})
export class ProfileComponent implements OnInit {
  readonly genderOptions = GENDER_OPTIONS;
  private readonly authService = inject(AuthService);
  private readonly studentService = inject(StudentService);
  private readonly institutionService = inject(InstitutionService);
  private readonly yearOfStudyService = inject(YearOfStudyService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly errorService = inject(ErrorHandlingService);
  private readonly cdr = inject(ChangeDetectorRef);

  currentUser: User | null = null;
  profileForm: FormGroup;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  isEditing = false;
  isSaving = false;
  institutions: readonly Institution[] = [];
  yearsOfStudy: readonly YearOfStudy[] = [];
  studentProfile: StudentProfile | null = null;

  // Error state
  loadError: ParsedError | null = null;
  saveError: ParsedError | null = null;
  isRetrying = false;

  constructor() {
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      studentNumber: ['', Validators.required],
      institutionId: ['', Validators.required],
      yearOfStudyId: ['', Validators.required],
      dateOfBirth: ['', Validators.required],
      gender: [''],
      specialRequirements: [''],
      // GH-239. Not `Validators.required` here, unlike profile-setup: this screen has to be
      // editable by students who created their profile before the field existed, and marking the
      // whole form invalid would lock them out of changing anything else. The backend's
      // submitApplication gate is what makes it required.
      emergencyContactName: ['', [Validators.maxLength(150)]],
      emergencyContactRelationship: ['', [Validators.maxLength(50)]],
      emergencyContactPhone: ['', [Validators.pattern(/^$|^\+?[0-9 ()\-]{7,20}$/)]],
      emergencyContactEmail: ['', [Validators.email]],
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    this.loadInstitutions();
    this.loadYearsOfStudy();
    this.loadStudentProfile();
  }

  loadStudentProfile() {
    if (this.currentUser) {
      this.loadError = null;
      this.isRetrying = true;

      this.studentService.getStudentProfile().subscribe({
        next: (profile) => {
          this.isRetrying = false;
          // Convert date string to Date object for the datepicker
          const dateOfBirth = profile.dateOfBirth ? parseLocalDate(profile.dateOfBirth) : null;
          // Anything other than male or female comes back empty, so the student picks again (#539)
          const normalizedGender = normalizeGender(profile.gender);

          this.studentProfile = {
            ...profile,
            institutionId: String(profile.institutionId),
            yearOfStudyId: String(profile.yearOfStudyId),
            gender: normalizedGender,
          };
          this.profileForm.patchValue({
            firstName: profile.firstName,
            lastName: profile.lastName,
            email: profile.email,
            phoneNumber: profile.phoneNumber,
            studentNumber: profile.studentNumber,
            institutionId: String(profile.institutionId),
            yearOfStudyId: String(profile.yearOfStudyId),
            dateOfBirth: dateOfBirth,
            gender: normalizedGender,
            specialRequirements: profile.specialRequirements || '',
            emergencyContactName: profile.emergencyContactName || '',
            emergencyContactRelationship: profile.emergencyContactRelationship || '',
            emergencyContactPhone: profile.emergencyContactPhone || '',
            emergencyContactEmail: profile.emergencyContactEmail || '',
          });
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.isRetrying = false;
          console.error('Error fetching student profile:', error);
          this.loadError = this.errorService.parseError(error);

          // This screen can only update a row, so without one it can never save (#465).
          // replaceUrl, so the back button does not land here and bounce straight forward again.
          if (this.loadError.statusCode === 404) {
            this.errorService.showWarning('Please complete your profile first.');
            this.router.navigate(['/profile-setup'], { replaceUrl: true });
          }
          this.cdr.markForCheck();
        },
      });
    }
  }

  retryLoadProfile(): void {
    this.loadStudentProfile();
    this.loadInstitutions();
    this.loadYearsOfStudy();
  }

  loadInstitutions() {
    this.institutionService.getInstitutions().subscribe({
      next: (response) => {
        // Handle both paginated and non-paginated responses
        this.institutions = contentOf(response);
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading institutions:', error);
        this.errorService.showWarning(
          'Could not load institutions. Some options may be unavailable.'
        );
        this.institutions = [];
        this.cdr.markForCheck();
      },
    });
  }

  loadYearsOfStudy() {
    this.yearOfStudyService.getYearsOfStudy().subscribe({
      next: (response) => {
        // Handle both paginated and non-paginated responses
        this.yearsOfStudy = contentOf(response);
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading years of study:', error);
        this.errorService.showWarning(
          'Could not load years of study. Some options may be unavailable.'
        );
        this.yearsOfStudy = [];
        this.cdr.markForCheck();
      },
    });
  }

  getInstitutionName(id: string): string {
    if (!id || !this.institutions || this.institutions.length === 0) {
      return 'Loading...';
    }
    const inst = this.institutions.find((i) => String(i.id) === String(id));
    return inst ? inst.name : 'Not found';
  }

  getYearOfStudyName(id: string): string {
    if (!id || !this.yearsOfStudy || this.yearsOfStudy.length === 0) {
      return 'Loading...';
    }
    const year = this.yearsOfStudy.find((y) => String(y.id) === String(id));
    return year ? year.yearOfStudy : 'Not found';
  }

  formatDateForDisplay(date: Date | string | null): string {
    if (!date) return 'Not specified';
    const dateObj = date instanceof Date ? date : parseLocalDate(date);
    if (isNaN(dateObj.getTime())) return 'Invalid date';
    return dateObj.toLocaleDateString('en-ZA', { year: 'numeric', month: 'long', day: 'numeric' });
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
    if (!this.isEditing && this.studentProfile) {
      // Reset form to original values
      const dateOfBirth = this.studentProfile.dateOfBirth
        ? parseLocalDate(this.studentProfile.dateOfBirth)
        : null;
      this.profileForm.patchValue({
        firstName: this.studentProfile.firstName,
        lastName: this.studentProfile.lastName,
        email: this.studentProfile.email,
        phoneNumber: this.studentProfile.phoneNumber,
        studentNumber: this.studentProfile.studentNumber,
        institutionId: this.studentProfile.institutionId,
        yearOfStudyId: this.studentProfile.yearOfStudyId,
        dateOfBirth: dateOfBirth,
        gender: this.studentProfile.gender || '',
        specialRequirements: this.studentProfile.specialRequirements || '',
        emergencyContactName: this.studentProfile.emergencyContactName || '',
        emergencyContactRelationship: this.studentProfile.emergencyContactRelationship || '',
        emergencyContactPhone: this.studentProfile.emergencyContactPhone || '',
        emergencyContactEmail: this.studentProfile.emergencyContactEmail || '',
      });
    }
  }

  onSubmit() {
    // Clear previous errors
    this.saveError = null;

    if (this.profileForm.valid && !this.isSaving) {
      this.isSaving = true;

      const formValue = this.profileForm.value;
      const dateOfBirth = toLocalDateString(formValue.dateOfBirth);
      const updateRequest: StudentRequest = {
        firstName: formValue.firstName,
        lastName: formValue.lastName,
        email: formValue.email,
        phoneNumber: formValue.phoneNumber,
        studentNumber: formValue.studentNumber,
        institutionId: Number(formValue.institutionId),
        yearOfStudyId: Number(formValue.yearOfStudyId),
        dateOfBirth: dateOfBirth,
        gender: formValue.gender || undefined,
        specialRequirements: formValue.specialRequirements || undefined,
        // Sent as '' rather than undefined when cleared: the backend reads null as "not sent,
        // leave alone" and an empty string as "remove it". This is someone else's data, so it
        // has to be withdrawable (GH-239).
        emergencyContactName: formValue.emergencyContactName ?? '',
        emergencyContactRelationship: formValue.emergencyContactRelationship ?? '',
        emergencyContactPhone: formValue.emergencyContactPhone ?? '',
        emergencyContactEmail: formValue.emergencyContactEmail ?? '',
      };

      this.studentService.updateStudentProfile(updateRequest).subscribe({
        next: (response) => {
          this.isSaving = false;
          this.isEditing = false;

          // Update local profile
          this.studentProfile = {
            ...response,
            institutionId: String(response.institutionId),
            yearOfStudyId: String(response.yearOfStudyId),
          };

          // Update current user in auth service. Guarded: spreading a null user built an object
          // missing id, profileCompleted and userRole, which `any` accepted as a User (GH-158).
          if (this.currentUser) {
            this.currentUser = {
              ...this.currentUser,
              firstName: response.firstName,
              lastName: response.lastName,
              email: response.email,
            };
          }

          this.errorService.showSuccess('Profile updated successfully!');
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.isSaving = false;
          console.error('Error updating profile:', error);
          this.saveError = this.errorService.parseError(error);

          // Don't show snackbar for validation errors - they'll see the inline error
          if (this.saveError.type !== 'validation') {
            this.errorService.showErrorSnackbar(this.saveError);
          }
          this.cdr.markForCheck();
        },
      });
    } else {
      // Mark all fields as touched to show validation errors
      Object.keys(this.profileForm.controls).forEach((key) => {
        this.profileForm.get(key)?.markAsTouched();
      });

      this.errorService.showWarning('Please fill in all required fields correctly');
    }
  }

  dismissSaveError(): void {
    this.saveError = null;
  }

  retrySave(): void {
    this.onSubmit();
  }

  openChangePasswordDialog() {
    const dialogRef = this.dialog.open(ChangePasswordDialogComponent, {
      width: '500px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === 'success') {
        // The backend blacklists the token this dialog just used to authenticate the change, so
        // the copy still held by the browser is already dead. Sign out and send the user back to
        // a fresh login instead of leaving them here to hit that dead token on the next request
        // (GH-291).
        this.authService.endSessionAfterPasswordChange();
        this.router.navigate(['/login'], { queryParams: { passwordChanged: 'true' } });
      }
    });
  }

  openDeleteAccountDialog() {
    const dialogRef = this.dialog.open(DeleteAccountDialogComponent, {
      width: '500px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === 'deleted') {
        this.errorService.showInfo('Account deleted successfully');
        // Logout and redirect to login
        this.authService.logout();
        this.router.navigate(['/login']);
      }
    });
  }
}
