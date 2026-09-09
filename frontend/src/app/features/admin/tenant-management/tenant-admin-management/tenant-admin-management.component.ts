import { Component, Input, OnInit, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TenantService, TenantAdminUser } from '../../../../core/services/tenant.service';

import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { DatePipe } from '@angular/common';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { SkeletonLoaderComponent } from '../../../../shared/components/skeleton-loader/skeleton-loader.component';
import { confirmAction } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

/** What the dialog form of this component is opened with. */
interface TenantAdminDialogData {
  tenantId: number;
  tenantName?: string;
}

@Component({
  selector: 'app-tenant-admin-management',
  templateUrl: './tenant-admin-management.component.html',
  styleUrls: ['./tenant-admin-management.component.scss'],
  standalone: true,
  imports: [
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatChipsModule,
    EmptyStateComponent,
    SkeletonLoaderComponent,
    DatePipe,
  ],
})
export class TenantAdminManagementComponent implements OnInit {
  @Input() tenantId!: number;
  @Input() tenantName = '';

  private tenantService = inject(TenantService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  // This component is used both as a routed page and as a dialog, so both tokens are optional.
  // `{ optional: true }` already selects the nullable overload; the `as any` that used to be here
  // defeated that overload resolution rather than helping it (GH-158).
  private dialogData = inject<TenantAdminDialogData | null>(MAT_DIALOG_DATA, { optional: true });
  dialogRef = inject(MatDialogRef<TenantAdminManagementComponent>, { optional: true });

  ngOnInit(): void {
    // When opened as a dialog, data comes via MAT_DIALOG_DATA
    if (this.dialogData) {
      this.tenantId = this.dialogData.tenantId;
      this.tenantName = this.dialogData.tenantName ?? '';
    }
    this.loadAdmins();
  }

  admins: TenantAdminUser[] = [];
  displayedColumns: string[] = [
    'name',
    'email',
    'createdAt',
    'mustChangePassword',
    'status',
    'actions',
  ];
  isLoading = false;

  loadAdmins(): void {
    this.isLoading = true;
    this.tenantService.listTenantAdmins(this.tenantId).subscribe({
      next: (admins) => {
        this.admins = admins;
        this.isLoading = false;
      },
      error: () => {
        this.snackBar.open('Failed to load admin users', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  openCreateDialog(): void {
    import('./tenant-admin-form-dialog/tenant-admin-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.TenantAdminFormDialogComponent, {
        width: '480px',
        data: { tenantId: this.tenantId, tenantName: this.tenantName },
      });
      dialogRef.afterClosed().subscribe((result) => {
        if (result) this.loadAdmins();
      });
    });
  }

  confirmDeactivate(admin: TenantAdminUser): void {
    confirmAction(this.dialog, {
      title: 'Deactivate this admin?',
      message: `Deactivate admin account for ${admin.firstName} ${admin.lastName} (${admin.email})? This will lock their account.`,
      confirmLabel: 'Deactivate',
      destructive: true,
    }).subscribe((confirmed) => {
      if (!confirmed) return;

      this.tenantService.deactivateTenantAdmin(this.tenantId, admin.id).subscribe({
        next: () => {
          this.snackBar.open('Admin account deactivated', 'Close', {
            duration: 3000,
            panelClass: 'snack-success',
          });
          this.loadAdmins();
        },
        error: (err) => {
          const msg = err?.error?.message || 'Failed to deactivate admin';
          this.snackBar.open(msg, 'Close', { duration: 4000 });
        },
      });
    });
  }

  isLocked(admin: TenantAdminUser): boolean {
    if (!admin.accountLockedUntil) return false;
    return new Date(admin.accountLockedUntil) > new Date();
  }
}
