import { Component, OnInit, inject } from '@angular/core';
import { TenantService, Tenant } from '../../../core/services/tenant.service';

import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { DatePipe, DecimalPipe } from '@angular/common';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';

@Component({
  selector: 'app-tenant-management',
  templateUrl: './tenant-management.component.html',
  styleUrls: ['./tenant-management.component.scss'],
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
    DecimalPipe,
  ],
})
export class TenantManagementComponent implements OnInit {
  private tenantService = inject(TenantService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  tenants: Tenant[] = [];
  displayedColumns: string[] = ['name', 'domain', 'status', 'createdAt', 'actions'];
  isLoading = false;

  ngOnInit(): void {
    this.loadTenants();
  }

  loadTenants(): void {
    this.isLoading = true;
    this.tenantService.getAllTenants(true).subscribe({
      next: (tenants) => {
        this.tenants = tenants;
        this.isLoading = false;
      },
      error: () => {
        this.snackBar.open('Failed to load tenants', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  openCreateDialog(): void {
    import('./tenant-form-dialog/tenant-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.TenantFormDialogComponent, {
        width: '520px',
        data: { mode: 'create' },
      });
      dialogRef.afterClosed().subscribe((result) => {
        if (result) this.loadTenants();
      });
    });
  }

  openAdminDialog(tenant: Tenant): void {
    import('./tenant-admin-management/tenant-admin-management.component').then((m) => {
      this.dialog.open(m.TenantAdminManagementComponent, {
        width: '860px',
        maxWidth: '95vw',
        data: { tenantId: tenant.id, tenantName: tenant.name },
        panelClass: 'tenant-admin-dialog',
      });
    });
  }

  openEditDialog(tenant: Tenant): void {
    import('./tenant-form-dialog/tenant-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.TenantFormDialogComponent, {
        width: '520px',
        data: { mode: 'edit', tenant },
      });
      dialogRef.afterClosed().subscribe((result) => {
        if (result) this.loadTenants();
      });
    });
  }

  toggleStatus(tenant: Tenant): void {
    const newStatus = !tenant.isActive;
    const label = newStatus ? 'activated' : 'deactivated';
    this.tenantService.toggleTenantStatus(tenant.id, newStatus).subscribe({
      next: (updated) => {
        const idx = this.tenants.findIndex((t) => t.id === tenant.id);
        if (idx !== -1) this.tenants[idx] = updated;
        this.tenants = [...this.tenants];
        this.snackBar.open(`Tenant ${label} successfully`, 'Close', {
          duration: 3000,
          panelClass: 'snack-success',
        });
      },
      error: (err) => {
        const msg =
          err?.error?.message || `Failed to ${newStatus ? 'activate' : 'deactivate'} tenant`;
        this.snackBar.open(msg, 'Close', { duration: 4000 });
      },
    });
  }

  isPlatformTenant(tenant: Tenant): boolean {
    return tenant.id === 0;
  }

  get activeTenantCount(): number {
    return this.tenants.filter((t) => t.isActive !== false).length;
  }
}
