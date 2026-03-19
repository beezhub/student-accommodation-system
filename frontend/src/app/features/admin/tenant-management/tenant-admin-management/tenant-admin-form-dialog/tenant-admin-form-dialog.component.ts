import { Component, inject } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { TenantService, TenantAdminRequest } from '../../../../../core/services/tenant.service';

import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-tenant-admin-form-dialog',
  templateUrl: './tenant-admin-form-dialog.component.html',
  styleUrls: ['./tenant-admin-form-dialog.component.scss'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatIconModule,
  ],
})
export class TenantAdminFormDialogComponent {
  private fb = inject(FormBuilder);
  private tenantService = inject(TenantService);
  dialogRef = inject<MatDialogRef<TenantAdminFormDialogComponent>>(MatDialogRef);
  data = inject(MAT_DIALOG_DATA);

  adminForm: FormGroup;
  isLoading = false;
  serverError: string | null = null;
  hidePassword = true;

  constructor() {
    this.adminForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
    });
  }

  onSubmit(): void {
    if (this.adminForm.invalid) {
      this.adminForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.serverError = null;

    const request: TenantAdminRequest = this.adminForm.value;

    this.tenantService.createTenantAdmin(this.data.tenantId, request).subscribe({
      next: () => {
        this.dialogRef.close(true);
      },
      error: (err) => {
        const status = err?.status;
        if (status === 409) {
          this.serverError = err?.error?.message || 'Email already in use or tenant is inactive.';
        } else {
          this.serverError = err?.error?.message || 'Failed to create admin user.';
        }
        this.isLoading = false;
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
