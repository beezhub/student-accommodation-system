import { Component, OnInit, inject } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import {
  InstitutionService,
  InstitutionResponse,
  InstitutionRequest,
} from '../../../../core/services/institution.service';

// Material imports
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-institution-form-dialog',
  templateUrl: './institution-form-dialog.component.html',
  styleUrls: ['./institution-form-dialog.component.css'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule,
  ],
})
export class InstitutionFormDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  private institutionService = inject(InstitutionService);
  private snackBar = inject(MatSnackBar);
  dialogRef = inject<MatDialogRef<InstitutionFormDialogComponent>>(MatDialogRef);
  data = inject(MAT_DIALOG_DATA);

  institutionForm: FormGroup;
  isLoading = false;
  mode: 'create' | 'edit';
  institution?: InstitutionResponse;

  constructor() {
    const data = this.data;

    this.mode = data.mode;
    this.institution = data.institution;

    this.institutionForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      address: ['', [Validators.required, Validators.maxLength(255)]],
      phoneNumber: ['', [Validators.required, Validators.maxLength(20)]],
      emailAddress: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    });
  }

  ngOnInit(): void {
    if (this.mode === 'edit' && this.institution) {
      this.institutionForm.patchValue({
        name: this.institution.name,
        address: this.institution.address,
        phoneNumber: this.institution.phoneNumber,
        emailAddress: this.institution.emailAddress,
      });
    }
  }

  onSubmit(): void {
    if (this.institutionForm.invalid) {
      this.institutionForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    const request: InstitutionRequest = this.institutionForm.value;

    const operation =
      this.mode === 'create'
        ? this.institutionService.createInstitution(request)
        : this.institutionService.updateInstitution(this.institution!.id, request);

    operation.subscribe({
      next: () => {
        const action = this.mode === 'create' ? 'created' : 'updated';
        this.snackBar.open(`Institution ${action} successfully`, 'Close', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (error) => {
        console.error(`Failed to ${this.mode} institution:`, error);
        const action = this.mode === 'create' ? 'create' : 'update';
        this.snackBar.open(`Failed to ${action} institution`, 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  getTitle(): string {
    return this.mode === 'create' ? 'Add New Institution' : 'Edit Institution';
  }

  getSubmitButtonText(): string {
    return this.mode === 'create' ? 'Create' : 'Update';
  }
}
