import { Component } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { AdminService } from '../../../../core/services/admin.service';

// Material imports
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../../../core/services/auth.service';
import { TenantService, Tenant } from '../../../../core/services/tenant.service';

@Component({
  selector: 'app-create-user-dialog',
  templateUrl: './create-user-dialog.component.html',
  styleUrls: ['./create-user-dialog.component.css'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
})
export class CreateUserDialogComponent {
  createUserForm: FormGroup;
  isSubmitting = false;
  hidePassword = true;
  isPlatformAdmin = false;
  tenants: Tenant[] = [];

  readonly availableRoles: string[] = ['PLATFORM_ADMIN', 'BUILDING_OWNER', 'BUILDING_STAFF'];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<CreateUserDialogComponent>,
    private adminService: AdminService,
    private snackBar: MatSnackBar,
    private authService: AuthService,
    private tenantService: TenantService
  ) {
    this.isPlatformAdmin = this.authService.currentUserValue?.tenantId === 0;

    this.createUserForm = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      userRole: ['BUILDING_OWNER', [Validators.required]],
    });

    if (this.isPlatformAdmin) {
      this.createUserForm.addControl('tenantId', this.fb.control(null, [Validators.required]));
      this.tenantService.getAllTenants().subscribe({
        next: (tenants) => this.tenants = tenants,
        error: () => this.snackBar.open('Failed to load tenants', 'Close', { duration: 3000 })
      });
    }
  }

  onSubmit(): void {
    if (this.createUserForm.valid) {
      this.isSubmitting = true;

      this.adminService.createUser(this.createUserForm.value).subscribe({
        next: (user) => {
          this.isSubmitting = false;
          this.snackBar.open('User created successfully!', 'Close', {
            duration: 3000,
            panelClass: ['success-snackbar'],
          });
          this.dialogRef.close(true);
        },
        error: (error) => {
          this.isSubmitting = false;
          const errorMessage =
            error.error?.message || error.error?.errors?.[0] || 'Failed to create user';
          this.snackBar.open(errorMessage, 'Close', {
            duration: 5000,
            panelClass: ['error-snackbar'],
          });
        },
      });
    } else {
      this.createUserForm.markAllAsTouched();
      this.snackBar.open('Please fill in all required fields correctly.', 'Close', {
        duration: 3000,
        panelClass: ['warning-snackbar'],
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }
}
