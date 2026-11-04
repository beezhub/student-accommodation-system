import { Component, OnInit, inject } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import {
  DocumentTypeService,
  DocumentTypeResponse,
  DocumentTypeRequest,
} from '../../../../core/services/document-type.service';

// Material imports
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-document-type-form-dialog',
  templateUrl: './document-type-form-dialog.component.html',
  styleUrls: ['./document-type-form-dialog.component.css'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule,
  ],
})
export class DocumentTypeFormDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  private documentTypeService = inject(DocumentTypeService);
  private snackBar = inject(MatSnackBar);
  dialogRef = inject<MatDialogRef<DocumentTypeFormDialogComponent>>(MatDialogRef);
  data = inject(MAT_DIALOG_DATA);

  documentTypeForm: FormGroup;
  isLoading = false;
  mode: 'create' | 'edit';
  documentType?: DocumentTypeResponse;

  constructor() {
    const data = this.data;

    this.mode = data.mode;
    this.documentType = data.documentType;

    this.documentTypeForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(50)]],
      description: ['', [Validators.maxLength(255)]],
      isRequired: [false, [Validators.required]],
    });
  }

  ngOnInit(): void {
    if (this.mode === 'edit' && this.documentType) {
      this.documentTypeForm.patchValue({
        name: this.documentType.name,
        description: this.documentType.description,
        isRequired: this.documentType.isRequired,
      });
    } else if (this.mode === 'create' && this.data.prefillName) {
      this.documentTypeForm.patchValue({ name: this.data.prefillName });
    }
  }

  getTitle(): string {
    if (this.data.shadow) return 'Override Platform Default';
    return this.mode === 'create' ? 'Add New Document Type' : 'Edit Document Type';
  }

  onSubmit(): void {
    if (this.documentTypeForm.invalid) {
      this.documentTypeForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    const request: DocumentTypeRequest = this.documentTypeForm.value;

    const operation =
      this.mode === 'create'
        ? this.documentTypeService.createDocumentType(request)
        : this.documentTypeService.updateDocumentType(this.documentType!.id, request);

    operation.subscribe({
      next: () => {
        const action = this.mode === 'create' ? 'created' : 'updated';
        this.snackBar.open(`Document type ${action} successfully`, 'Close', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (error) => {
        console.error(`Failed to ${this.mode} document type:`, error);
        const action = this.mode === 'create' ? 'create' : 'update';
        this.snackBar.open(`Failed to ${action} document type`, 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  getSubmitButtonText(): string {
    if (this.data.shadow) return 'Create Override';
    return this.mode === 'create' ? 'Create' : 'Update';
  }
}
