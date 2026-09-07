import { Component, inject } from '@angular/core';

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
  private fb = inject(FormBuilder);
  private dialogRef = inject<MatDialogRef<CreateUserDialogComponent>>(MatDialogRef);
  private adminService = inject(AdminService);
  private snackBar = inject(MatSnackBar);

  createUserForm: FormGroup;
  isSubmitting = false;
  hidePassword = true;
  isPlatformAdmin = false;
  tenants: Tenant[] = [];

  /**
   * ADMIN is gone: it is the deprecated role, and AppUserMapper rejects it outright, so that
   * option failed every time it was chosen whatever tenant was picked. TENANT_ADMIN is the role
   * the tenant-scoping rule is actually written for and was not selectable at all.
   */
  readonly availableRoles: string[] = [
    'PLATFORM_ADMIN',
    'PLATFORM_STAFF',
    'TENANT_ADMIN',
    'BUILDING_STAFF',
  ];

  /**
   * Roles the backend requires a real institution tenant for (GH-146). PLATFORM_STAFF is absent
   * on purpose: like PLATFORM_ADMIN it must be tenant 0, and resolveTenantForRole refuses to
   * create one carrying a company (GH-238).
   */
  private static readonly TENANT_SCOPED_ROLES = ['TENANT_ADMIN', 'BUILDING_STAFF'];

  private authService = inject(AuthService);
  private tenantService = inject(TenantService);

  constructor() {
    this.isPlatformAdmin = this.authService.currentUserValue?.tenantId === 0;
    this.createUserForm = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      // Was BUILDING_OWNER, which is in neither availableRoles nor the backend UserRole, so the
      // form opened on a value that could never be submitted.
      userRole: ['BUILDING_STAFF', [Validators.required]],
    });

    if (this.isPlatformAdmin) {
      this.createUserForm.addControl('tenantId', this.fb.control(null, [Validators.required]));
      this.tenantService.getAllTenants().subscribe({
        next: (tenants) => (this.tenants = tenants),
        error: () => this.snackBar.open('Failed to load tenants', 'Close', { duration: 3000 }),
      });

      // Without this, choosing a tenant and then switching to PLATFORM_ADMIN submits a stale
      // non-zero tenantId, which the backend now rejects (GH-146). The control keeps a value
      // that is no longer among the options, so the user cannot see what is wrong.
      this.createUserForm.get('userRole')?.valueChanges.subscribe((role: string) => {
        const tenantControl = this.createUserForm.get('tenantId');
        if (!tenantControl) return;
        tenantControl.reset(null);
        if (this.isTenantScoped(role)) {
          tenantControl.setValidators([Validators.required]);
        } else {
          tenantControl.clearValidators();
        }
        tenantControl.updateValueAndValidity();
      });
    }
  }

  private isTenantScoped(role: string): boolean {
    return CreateUserDialogComponent.TENANT_SCOPED_ROLES.includes(role);
  }

  /** The tenant control is meaningless for a platform admin, who must be tenant 0. */
  get showTenantField(): boolean {
    return this.isPlatformAdmin && this.isTenantScoped(this.createUserForm.get('userRole')?.value);
  }

  /**
   * The platform tenant is never a valid choice for a tenant-scoped role: 0 is the sentinel that
   * disables the tenant filter, which is the privilege escalation in GH-146.
   */
  get availableTenantsForRole(): Tenant[] {
    return this.tenants.filter((t) => t.id !== 0);
  }

  onSubmit(): void {
    if (this.createUserForm.valid) {
      this.isSubmitting = true;

      this.adminService.createUser(this.createUserForm.value).subscribe({
        next: () => {
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
