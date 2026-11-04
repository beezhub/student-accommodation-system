import { Component, OnInit, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import {
  TenantService,
  Tenant,
  TenantAdminUser,
} from '../../../../../core/services/tenant.service';

import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';

/** Platform tenant id; tenant admins can never live there. */
const PLATFORM_TENANT_ID = 0;

export interface MoveTenantAdminDialogData {
  tenantId: number;
  tenantName: string;
  admin: TenantAdminUser;
}

@Component({
  selector: 'app-move-tenant-admin-dialog',
  templateUrl: './move-tenant-admin-dialog.component.html',
  // Same look as the sibling "add admin" dialog, so it shares that stylesheet.
  styleUrls: ['../add-existing-admin-dialog/add-existing-admin-dialog.component.scss'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatIconModule,
  ],
})
export class MoveTenantAdminDialogComponent implements OnInit {
  private tenantService = inject(TenantService);
  dialogRef = inject<MatDialogRef<MoveTenantAdminDialogComponent>>(MatDialogRef);
  data = inject<MoveTenantAdminDialogData>(MAT_DIALOG_DATA);

  targetTenantId = new FormControl<number | null>(null, Validators.required);
  targetTenants: Tenant[] = [];
  tenantsLoaded = false;
  isLoading = false;
  serverError: string | null = null;

  ngOnInit(): void {
    // Active only: the backend refuses a deactivated company, so offering one is a dead end.
    this.tenantService.getActiveTenants().subscribe({
      next: (tenants) => {
        this.targetTenants = tenants.filter(
          (t) => t.id !== PLATFORM_TENANT_ID && t.id !== this.data.tenantId
        );
        this.tenantsLoaded = true;
      },
      error: () => {
        this.serverError = 'Failed to load property companies.';
      },
    });
  }

  onSubmit(): void {
    const targetTenantId = this.targetTenantId.value;
    if (targetTenantId === null) {
      this.targetTenantId.markAsTouched();
      return;
    }

    this.isLoading = true;
    this.serverError = null;

    this.tenantService
      .moveTenantAdmin(this.data.tenantId, this.data.admin.id, targetTenantId)
      .subscribe({
        next: () => this.dialogRef.close(true),
        error: (err) => {
          this.serverError = err?.error?.message || 'Failed to move admin.';
          this.isLoading = false;
        },
      });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
