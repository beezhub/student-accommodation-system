import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ApplicationService } from '../../../core/services/application.service';
import { StudentService } from '../../../core/services/student.service';
import { InstitutionService } from '../../../core/services/institution.service';
import { MatSnackBar } from '@angular/material/snack-bar';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-application-submission',
  templateUrl: './application-submission.component.html',
  styleUrls: ['./application-submission.component.css'],
  standalone: true,
  imports: [
    CommonModule,
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
    MatSnackBarModule,
    MatIconModule
  ]
})
export class ApplicationSubmissionComponent implements OnInit {
  applicationForm: FormGroup;
  isSubmitting = false;
  currentUser: any;
  studentId: number | null = null;
  institutionName: string = '';

  // Accommodation types for the dropdown
  accommodationTypes = [
    { value: 'SINGLE', viewValue: 'Single Room' },
    { value: 'DOUBLE', viewValue: 'Double Room' },
    { value: 'STUDIO', viewValue: 'Studio Apartment' },
    { value: 'ENSUITE', viewValue: 'En-suite Room' }
  ];

  // Room type options for the dropdown
  roomTypeOptions = [
    { value: 'STANDARD', viewValue: 'Standard Room' },
    { value: 'PREMIUM', viewValue: 'Premium Room' },
    { value: 'ACCESSIBLE', viewValue: 'Accessible Room' },
    { value: 'QUIET_FLOOR', viewValue: 'Quiet Study Floor' }
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private applicationService: ApplicationService,
    private studentService: StudentService,
    private institutionService: InstitutionService,
    private snackBar: MatSnackBar
  ) {
    this.applicationForm = this.fb.group({
      preferredAccommodationType: ['', Validators.required],
      preferredMoveInDate: ['', Validators.required],
      preferredDurationMonths: ['', [Validators.required, Validators.min(1), Validators.max(12)]],
      preferredRoomType: ['', Validators.required],
      specialRequirements: [''],
      courseName: ['', Validators.required]
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
      return;
    }

    // Get student profile to retrieve student ID and institution
    this.studentService.getStudentProfile().subscribe({
      next: (profile) => {
        this.studentId = profile.id;

        // Get institution name
        if (profile.institutionId) {
          this.institutionService.getInstitutions().subscribe({
            next: (response: any) => {
              const institutions = Array.isArray((response as any).content)
                ? (response as any).content
                : (Array.isArray(response) ? response : []);

              const institution = institutions.find((inst: any) =>
                String(inst.id) === String(profile.institutionId)
              );

              if (institution) {
                this.institutionName = institution.name;
              }
            },
            error: (error) => {
              console.error('Error fetching institution:', error);
            }
          });
        }
      },
      error: (error) => {
        console.error('Error fetching student profile:', error);
        this.snackBar.open('Failed to load student profile. Please complete your profile first.', 'Close', {
          duration: 5000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
        });
        this.router.navigate(['/profile-setup']);
      }
    });
  }

  onSubmit() {
    if (!this.studentId) {
      this.snackBar.open('Student profile not found. Please complete your profile first.', 'Close', {
        duration: 5000,
        horizontalPosition: 'end',
        verticalPosition: 'top',
      });
      this.router.navigate(['/profile-setup']);
      return;
    }

    if (this.applicationForm.valid) {
      this.isSubmitting = true;

      const formValue = this.applicationForm.value;

      // Format the date to YYYY-MM-DD format for the backend
      const moveInDate = formValue.preferredMoveInDate;
      const formattedDate = moveInDate instanceof Date
        ? moveInDate.toISOString().split('T')[0]
        : moveInDate;

      // Ensure numeric fields are sent as numbers
      const durationMonths = formValue.preferredDurationMonths;
      const parsedDuration = typeof durationMonths === 'string'
        ? parseInt(durationMonths, 10)
        : durationMonths;

      const applicationData = {
        studentId: this.studentId,
        preferredAccommodationType: formValue.preferredAccommodationType,
        preferredMoveInDate: formattedDate,
        preferredDurationMonths: parsedDuration,
        preferredRoomType: formValue.preferredRoomType,
        specialRequirements: formValue.specialRequirements || null,
        institutionName: this.institutionName || 'Not specified',
        courseName: formValue.courseName
      };

      this.applicationService.createApplication(applicationData).subscribe({
        next: (response) => {
          this.isSubmitting = false;
          this.snackBar.open('Application submitted successfully!', 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
            panelClass: ['success-snackbar']
          });
          this.router.navigate(['/document-upload']);
        },
        error: (error) => {
          this.isSubmitting = false;
          console.error('Error submitting application:', error);

          const errorMessage = error.error?.message || 'Failed to submit application. Please try again.';
          this.snackBar.open(errorMessage, 'Close', {
            duration: 5000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
            panelClass: ['error-snackbar']
          });
        }
      });
    } else {
      this.applicationForm.markAllAsTouched();
      this.snackBar.open('Please fill in all required fields correctly', 'Close', {
        duration: 3000,
        horizontalPosition: 'end',
        verticalPosition: 'top',
      });
    }
  }

  skipApplication() {
    this.router.navigate(['/document-upload']);
  }
}
