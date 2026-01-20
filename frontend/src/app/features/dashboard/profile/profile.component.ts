import { Component, OnInit } from '@angular/core';

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
import { ErrorDisplayComponent } from '../../../shared/error-display/error-display.component';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
  standalone: true,
  imports: [RouterModule, ReactiveFormsModule, MaterialModule, ErrorDisplayComponent],
})
export class ProfileComponent implements OnInit {
  currentUser: any;
  profileForm: FormGroup;
  isEditing = false;
  isSaving = false;
  institutions: Institution[] = [];
  yearsOfStudy: YearOfStudy[] = [];
  studentProfile: StudentProfile | null = null;

  // Error state
  loadError: ParsedError | null = null;
  saveError: ParsedError | null = null;
  isRetrying = false;

  constructor(
    private authService: AuthService,
    private studentService: StudentService,
    private institutionService: InstitutionService,
    private yearOfStudyService: YearOfStudyService,
    private fb: FormBuilder,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private router: Router,
    private errorService: ErrorHandlingService
  ) {
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
          this.studentProfile = {
            ...profile,
            institutionId: String(profile.institutionId),
            yearOfStudyId: String(profile.yearOfStudyId),
          };
          this.profileForm.patchValue({
            firstName: profile.firstName,
            lastName: profile.lastName,
            email: profile.email,
            phoneNumber: profile.phoneNumber,
            studentNumber: profile.studentNumber,
            institutionId: String(profile.institutionId),
            yearOfStudyId: String(profile.yearOfStudyId),
            dateOfBirth: profile.dateOfBirth,
            gender: profile.gender || '',
            specialRequirements: profile.specialRequirements || '',
          });
        },
        error: (error) => {
          this.isRetrying = false;
          console.error('Error fetching student profile:', error);
          this.loadError = this.errorService.parseError(error);
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
        console.log('Institutions API response:', response);
        // Handle both paginated and non-paginated responses
        if (response && Array.isArray((response as any).content)) {
          this.institutions = (response as any).content;
        } else if (Array.isArray(response)) {
          this.institutions = response;
        } else {
          this.institutions = [];
        }
        console.log('Loaded institutions:', this.institutions);
      },
      error: (error) => {
        console.error('Error loading institutions:', error);
        this.institutions = [];
      },
    });
  }

  loadYearsOfStudy() {
    this.yearOfStudyService.getYearsOfStudy().subscribe({
      next: (response) => {
        console.log('Years of Study API response:', response);
        // Handle both paginated and non-paginated responses
        if (response && Array.isArray((response as any).content)) {
          this.yearsOfStudy = (response as any).content;
        } else if (Array.isArray(response)) {
          this.yearsOfStudy = response;
        } else {
          this.yearsOfStudy = [];
        }
        console.log('Loaded years of study:', this.yearsOfStudy);
      },
      error: (error) => {
        console.error('Error loading years of study:', error);
        this.yearsOfStudy = [];
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

  toggleEdit() {
    this.isEditing = !this.isEditing;
    if (!this.isEditing && this.studentProfile) {
      // Reset form to original values
      this.profileForm.patchValue({
        firstName: this.studentProfile.firstName,
        lastName: this.studentProfile.lastName,
        email: this.studentProfile.email,
        phoneNumber: this.studentProfile.phoneNumber,
        studentNumber: this.studentProfile.studentNumber,
        institutionId: this.studentProfile.institutionId,
        yearOfStudyId: this.studentProfile.yearOfStudyId,
        dateOfBirth: this.studentProfile.dateOfBirth,
        gender: this.studentProfile.gender || '',
        specialRequirements: this.studentProfile.specialRequirements || '',
      });
    }
  }

  onSubmit() {
    // Clear previous errors
    this.saveError = null;

    if (this.profileForm.valid && !this.isSaving) {
      this.isSaving = true;

      const formValue = this.profileForm.value;
      const updateRequest: StudentRequest = {
        firstName: formValue.firstName,
        lastName: formValue.lastName,
        email: formValue.email,
        phoneNumber: formValue.phoneNumber,
        studentNumber: formValue.studentNumber,
        institutionId: Number(formValue.institutionId),
        yearOfStudyId: Number(formValue.yearOfStudyId),
        dateOfBirth: formValue.dateOfBirth,
        gender: formValue.gender || undefined,
        specialRequirements: formValue.specialRequirements || undefined,
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

          // Update current user in auth service
          this.currentUser = {
            ...this.currentUser,
            firstName: response.firstName,
            lastName: response.lastName,
            email: response.email,
          };

          this.errorService.showSuccess('Profile updated successfully!');
        },
        error: (error) => {
          this.isSaving = false;
          console.error('Error updating profile:', error);
          this.saveError = this.errorService.parseError(error);

          // Don't show snackbar for validation errors - they'll see the inline error
          if (this.saveError.type !== 'validation') {
            this.errorService.showErrorSnackbar(this.saveError);
          }
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
        this.errorService.showSuccess('Password changed successfully!');
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
