import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { TenantService, Tenant, TenantRequest } from '../../../../core/services/tenant.service';

import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-tenant-form-dialog',
  templateUrl: './tenant-form-dialog.component.html',
  styleUrls: ['./tenant-form-dialog.component.scss'],
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
export class TenantFormDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  private tenantService = inject(TenantService);
  dialogRef = inject<MatDialogRef<TenantFormDialogComponent>>(MatDialogRef);
  data = inject(MAT_DIALOG_DATA);

  tenantForm: FormGroup;
  isLoading = false;
  serverError: string | null = null;
  mode: 'create' | 'edit';
  tenant?: Tenant;

  constructor() {
    this.mode = this.data.mode;
    this.tenant = this.data.tenant;

    this.tenantForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(200)]],
      domain: ['', Validators.maxLength(255)],
    });
  }

  ngOnInit(): void {
    if (this.mode === 'edit' && this.tenant) {
      this.tenantForm.patchValue({
        name: this.tenant.name,
        domain: this.tenant.domain || '',
      });
    }
  }

  onSubmit(): void {
    if (this.tenantForm.invalid) {
      this.tenantForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.serverError = null;

    const request: TenantRequest = {
      name: this.tenantForm.value.name,
      domain: this.tenantForm.value.domain || undefined,
    };

    const operation =
      this.mode === 'create'
        ? this.tenantService.createTenant(request)
        : this.tenantService.updateTenant(this.tenant!.id, request);

    operation.subscribe({
      next: () => {
        this.dialogRef.close(true);
      },
      error: (err) => {
        const status = err?.status;
        if (status === 409) {
          this.serverError = 'A tenant with this name already exists.';
        } else {
          this.serverError = err?.error?.message || `Failed to ${this.mode} tenant.`;
        }
        this.isLoading = false;
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  getTitle(): string {
    return this.mode === 'create' ? 'New Tenant' : 'Edit Tenant';
  }
}
