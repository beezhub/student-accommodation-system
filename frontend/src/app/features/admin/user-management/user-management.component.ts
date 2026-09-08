import { Component, OnInit, inject } from '@angular/core';

import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { AdminService, AdminUser } from '../../../core/services/admin.service';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CreateUserDialogComponent } from './create-user-dialog/create-user-dialog.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { TenantService, Tenant } from '../../../core/services/tenant.service';
import { confirmAction } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { RoleService } from '../../../core/services/role.service';
import { ROLE, UserRoleValue } from '../../../core/models/role.model';

@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatTooltipModule,
    SkeletonLoaderComponent,
    EmptyStateComponent,
  ],
})
export class UserManagementComponent implements OnInit {
  private adminService = inject(AdminService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  users: AdminUser[] = [];
  displayedColumns: string[] = ['id', 'name', 'email', 'role', 'mustChangePassword', 'actions'];
  isLoading = false;
  filterRole: string = 'all';
  // Server-side paging: the list used to arrive whole, however many accounts existed (GH-157).
  pageIndex = 0;
  pageSize = 25;
  totalUsers = 0;
  isPlatformAdmin = false;
  tenants: Tenant[] = [];
  filterTenantId: number | 'all' = 'all';
  filtersVisible = false;

  get activeFilterCount(): number {
    return this.filterRole !== 'all' ? 1 : 0;
  }

  /**
   * Counts over the page currently shown, by the roles that actually exist.
   *
   * <p>These counted `'ADMIN'` and `'LANDLORD'` before. `LANDLORD` is not a role in this system at
   * all, so that chip always read zero, and `ADMIN` is the deprecated one kept only for migration,
   * so the admin count missed every PLATFORM_ADMIN, TENANT_ADMIN and BUILDING_STAFF (GH-158).
   */
  private countOnPage(...roles: UserRoleValue[]): number {
    const wanted = roles.map((role) => role.toUpperCase());
    return this.users.filter((u) => wanted.includes((u.userRole ?? '').toUpperCase())).length;
  }

  get platformAdminCount(): number {
    return this.countOnPage(ROLE.PLATFORM_ADMIN, ROLE.ADMIN);
  }

  get tenantAdminCount(): number {
    return this.countOnPage(ROLE.TENANT_ADMIN);
  }

  get buildingStaffCount(): number {
    return this.countOnPage(ROLE.BUILDING_STAFF);
  }

  get studentCount(): number {
    return this.countOnPage(ROLE.STUDENT);
  }

  toggleFilters(): void {
    this.filtersVisible = !this.filtersVisible;
  }

  private roleService = inject(RoleService);
  private tenantService = inject(TenantService);

  constructor() {
    // Was `tenantId === 0`. That is the tenant sentinel, not a role: students carry tenant 0 too,
    // and a platform admin's authority comes from their role (GH-158).
    this.isPlatformAdmin = this.roleService.isPlatformAdmin();
  }

  ngOnInit(): void {
    if (this.isPlatformAdmin) {
      this.tenantService.getAllTenants().subscribe({
        next: (tenants) => (this.tenants = tenants),
      });
    }
    this.loadUsers();
  }
  loadUsers(): void {
    this.isLoading = true;

    const page$ =
      this.filterRole === 'all'
        ? this.adminService.getAllUsers(this.pageIndex, this.pageSize)
        : this.adminService.getUsersByRole(this.filterRole, this.pageIndex, this.pageSize);

    page$.subscribe({
      next: (page) => {
        this.users = page.content;
        this.totalUsers = page.totalElements;
        this.isLoading = false;
      },
      error: (_err) => {
        this.isLoading = false;
        this.snackBar.open('Failed to load users', 'Close', {
          duration: 3000,
          panelClass: ['error-snackbar'],
        });
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadUsers();
  }

  onFilterChange(role: string): void {
    this.filterRole = role;
    // A new filter is a new result set, so go back to the first page rather than asking for a
    // page number that may not exist in it.
    this.pageIndex = 0;
    this.loadUsers();
  }

  openCreateUserDialog(): void {
    const dialogRef = this.dialog.open(CreateUserDialogComponent, {
      width: '500px',
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadUsers();
      }
    });
  }

  deleteUser(user: AdminUser): void {
    confirmAction(this.dialog, {
      title: 'Delete this user?',
      message: `${user.email} loses access immediately. This cannot be undone.`,
      confirmLabel: 'Delete user',
      destructive: true,
    }).subscribe((confirmed) => {
      if (!confirmed) return;
      this.adminService.deleteUser(user.id).subscribe({
        next: () => {
          this.snackBar.open('User deleted successfully', 'Close', {
            duration: 3000,
            panelClass: ['success-snackbar'],
          });
          this.loadUsers();
        },
        error: (error) => {
          const errorMessage =
            error.error?.message || error.error?.errors?.[0] || 'Failed to delete user';
          this.snackBar.open(errorMessage, 'Close', {
            duration: 5000,
            panelClass: ['error-snackbar'],
          });
        },
      });
    });
  }

  getRoleColor(role: string): string {
    switch (role) {
      case 'PLATFORM_ADMIN':
        return 'primary';
      case 'PLATFORM_STAFF':
        return 'primary';
      case 'BUILDING_OWNER':
        return 'accent';
      case 'BUILDING_STAFF':
        return 'accent';
      case 'STUDENT':
        return 'warn';
      default:
        return '';
    }
  }
}
