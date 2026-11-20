import { Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { AcademicYear } from '../../../../core/models/academic-year.model';
import { AcademicYearService } from '../../../../core/services/academic-year.service';
import { Tenant, TenantService } from '../../../../core/services/tenant.service';
import { PLATFORM_TENANT_ID } from '../academic-year-wording';

export interface AssignAcademicYearDialogData {
  academicYear: AcademicYear;
}

/**
 * "Assign to company" (GH-575): the company gets its own copy of a platform year and its
 * intakes, with the same dates. The platform year stays, so it can go to several companies.
 * Closes with the company's new year.
 */
@Component({
  selector: 'app-assign-academic-year-dialog',
  templateUrl: './assign-academic-year-dialog.component.html',
  // Same look as the create and edit dialog next to it.
  styleUrls: ['../academic-year-form-dialog/academic-year-form-dialog.component.css'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
})
export class AssignAcademicYearDialogComponent implements OnInit {
  private academicYearService = inject(AcademicYearService);
  private tenantService = inject(TenantService);
  private dialogRef =
    inject<MatDialogRef<AssignAcademicYearDialogComponent, AcademicYear>>(MatDialogRef);
  data = inject<AssignAcademicYearDialogData>(MAT_DIALOG_DATA);

  form = new FormGroup({
    companyId: new FormControl<number | null>(null, Validators.required),
  });
  readonly companyId = this.form.controls.companyId;
  companies: Tenant[] = [];
  companiesLoaded = false;
  isSubmitting = false;
  serverError: string | null = null;

  ngOnInit(): void {
    // Active only: the backend refuses a deactivated company, so offering one is a dead end.
    this.tenantService.getActiveTenants().subscribe({
      next: (tenants) => {
        this.companies = tenants.filter((t) => t.id !== PLATFORM_TENANT_ID);
        this.companiesLoaded = true;
      },
      error: () => (this.serverError = 'The companies could not be loaded.'),
    });
  }

  onSubmit(): void {
    const companyId = this.companyId.value;
    if (companyId === null) {
      this.companyId.markAsTouched();
      return;
    }
    this.isSubmitting = true;
    this.serverError = null;
    this.academicYearService.assignToCompany(this.data.academicYear.id, companyId).subscribe({
      next: (copy) => this.dialogRef.close(copy),
      error: (error: HttpErrorResponse) => {
        this.serverError = error?.error?.message || 'The year could not be assigned.';
        this.isSubmitting = false;
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
