import { Component, OnInit, inject } from '@angular/core';

import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { AdminService, AdminUser } from '../../../core/services/admin.service';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
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
import { AuthService } from '../../../core/services/auth.service';
import { TenantService, Tenant } from '../../../core/services/tenant.service';

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
  isPlatformAdmin = false;
  tenants: Tenant[] = [];
  filterTenantId: number | 'all' = 'all';
  filtersVisible = false;

  get activeFilterCount(): number {
    return this.filterRole !== 'all' ? 1 : 0;
  }

  get adminCount(): number {
    return this.users.filter((u) => u.userRole === 'ADMIN').length;
  }

  get landlordCount(): number {
    return this.users.filter((u) => u.userRole === 'LANDLORD').length;
  }

  get studentCount(): number {
    return this.users.filter((u) => u.userRole === 'STUDENT').length;
  }

  toggleFilters(): void {
    this.filtersVisible = !this.filtersVisible;
  }

  private authService = inject(AuthService);
  private tenantService = inject(TenantService);

  constructor() {
    this.isPlatformAdmin = this.authService.currentUserValue?.tenantId === 0;
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

    if (this.filterRole === 'all') {
      this.adminService.getAllUsers().subscribe({
        next: (users) => {
          this.users = users;
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
    } else {
      this.adminService.getUsersByRole(this.filterRole).subscribe({
        next: (users) => {
          this.users = users;
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
  }

  onFilterChange(role: string): void {
    this.filterRole = role;
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
    if (confirm(`Are you sure you want to delete user ${user.email}?`)) {
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
    }
  }

  getRoleColor(role: string): string {
    switch (role) {
      case 'PLATFORM_ADMIN':
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
