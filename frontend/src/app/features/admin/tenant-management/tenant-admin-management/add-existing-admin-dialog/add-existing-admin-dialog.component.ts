import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TenantService, UnassignedAdmin } from '../../../../../core/services/tenant.service';

/** The only role this screen manages. BUILDING_STAFF also waits for a company but has no place here. */
const COMPANY_ADMIN_ROLE = 'TENANT_ADMIN';

export interface AddExistingAdminDialogData {
  tenantId: number;
  tenantName: string;
}

/**
 * Adds an existing account as an admin of a company (#413). Accounts are created in user
 * management only; this offers the tenant admins made there with no company yet. They wait locked,
 * and assigning one gives it this company and unlocks it in one step.
 */
@Component({
  selector: 'app-add-existing-admin-dialog',
  templateUrl: './add-existing-admin-dialog.component.html',
  styleUrls: ['./add-existing-admin-dialog.component.scss'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    DatePipe,
    MatDialogModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
})
export class AddExistingAdminDialogComponent implements OnInit {
  private tenantService = inject(TenantService);
  dialogRef = inject<MatDialogRef<AddExistingAdminDialogComponent>>(MatDialogRef);
  data = inject<AddExistingAdminDialogData>(MAT_DIALOG_DATA);

  /** What is typed, or the admin picked from the list. */
  search = new FormControl<string | UnassignedAdmin>('', { nonNullable: true });
  waiting: UnassignedAdmin[] = [];
  loaded = false;
  loadError: string | null = null;
  chosen: UnassignedAdmin | null = null;
  isSaving = false;
  serverError: string | null = null;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loadError = null;
    this.tenantService.listUnassignedAdmins().subscribe({
      next: (admins) => {
        this.waiting = admins.filter((admin) => admin.userRole === COMPANY_ADMIN_ROLE);
        this.loaded = true;
      },
      error: () => {
        this.loadError =
          'The list of admins waiting for a company could not be loaded. Close this and try again.';
        this.loaded = true;
      },
    });
  }

  /** Waiting admins whose name or email contains the typed text, ignoring case. */
  matching(term: string): UnassignedAdmin[] {
    const needle = term.trim().toLowerCase();
    if (!needle) return this.waiting;
    return this.waiting.filter(
      (admin) =>
        this.fullName(admin).toLowerCase().includes(needle) ||
        admin.email.toLowerCase().includes(needle)
    );
  }

  get options(): UnassignedAdmin[] {
    const value = this.search.value;
    return this.matching(typeof value === 'string' ? value : '');
  }

  fullName(admin: UnassignedAdmin): string {
    return `${admin.firstName} ${admin.lastName}`;
  }

  /** What the search box shows once an admin is picked. */
  displayName = (value: UnassignedAdmin | string | null): string =>
    value && typeof value !== 'string' ? this.fullName(value) : value ?? '';

  choose(admin: UnassignedAdmin): void {
    this.chosen = admin;
    this.serverError = null;
  }

  /** Typing again after picking means a different search, so the pick no longer stands. */
  onType(): void {
    this.chosen = null;
  }

  confirm(): void {
    const admin = this.chosen;
    if (!admin || this.isSaving) return;
    this.isSaving = true;
    this.serverError = null;
    this.tenantService.assignAdmin(admin.id, this.data.tenantId).subscribe({
      next: () => this.dialogRef.close(admin),
      error: (err: HttpErrorResponse) => {
        this.isSaving = false;
        if (err.status === 409) {
          // Someone assigned this account a moment ago, so it is no longer waiting.
          this.serverError = `${this.fullName(
            admin
          )} was just added to another company. The list is up to date again.`;
          this.chosen = null;
          this.search.setValue('');
          this.load();
          return;
        }
        this.serverError =
          err.error?.message || `${this.fullName(admin)} could not be added. Try again.`;
      },
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
