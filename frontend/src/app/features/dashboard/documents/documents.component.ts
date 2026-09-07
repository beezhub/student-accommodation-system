import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { DocumentViewerComponent } from './document-viewer/document-viewer.component';
import { DocumentService } from '../../../core/services/document.service';
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';
import { DocumentTypeService } from '../../../core/services/document-type.service';
import { DocumentType } from '../../../core/models/document-type.model';
import { ErrorHandlingService, ParsedError } from '../../../core/services/error-handling.service';
import { ErrorDisplayComponent } from '../../../shared/error-display/error-display.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

// Material imports
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { confirmAction } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { User } from '../../../core/models/user.model';
import { DocumentData, DocumentPreview } from '../../../core/models/document.model';
import { contentOf } from '../../../core/models/page-response.model';

/** Merged view of a document type slot — either empty or populated by an uploaded document. */
export interface DocumentSlot {
  typeId: number;
  typeName: string;
  isRequired: boolean;
  documentId: number | null;
  documentName: string | null;
  status: 'NOT_UPLOADED' | 'PENDING' | 'APPROVED' | 'REJECTED';
  uploadDate: string | null;
  fileSize: string | null;
}

@Component({
  selector: 'app-documents',
  templateUrl: './documents.component.html',
  styleUrls: ['./documents.component.scss'],
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    DocumentViewerComponent,
    // Material modules
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressBarModule,
    MatTooltipModule,
    ModernStatusBadgeComponent,
    ErrorDisplayComponent,
    SkeletonLoaderComponent,
    EmptyStateComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentsComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly documentService = inject(DocumentService);
  private readonly documentTypeService = inject(DocumentTypeService);
  private readonly errorService = inject(ErrorHandlingService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly dialog = inject(MatDialog);

  currentUser: User | null = null;
  selectedDocument: DocumentPreview | null = null;
  isUploading = false;
  isLoading = true;

  showUploadModal = false;
  selectedFile: File | null = null;
  uploadForm: FormGroup;
  /** The slot being targeted for upload/replace. */
  activeSlot: DocumentSlot | null = null;

  // Error state
  loadError: ParsedError | null = null;
  uploadError: ParsedError | null = null;
  fileValidationErrors: string[] = [];

  // Upload progress
  uploadProgress = 0;
  uploadStatus: 'idle' | 'validating' | 'uploading' | 'complete' | 'error' = 'idle';

  /** Merged pack view — one entry per document type. */
  slots: DocumentSlot[] = [];

  allDocumentTypes: DocumentType[] = [];

  constructor() {
    this.uploadForm = this.fb.group({
      documentType: ['', Validators.required],
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    this.loadPack();
  }

  /** Load document types and uploaded documents, then merge into slots. */
  loadPack(): void {
    this.isLoading = true;
    this.loadError = null;

    this.documentTypeService.getAllDocumentTypes().subscribe({
      next: (types) => {
        this.allDocumentTypes = contentOf(types);

        // Now load uploaded documents
        this.documentService.getUploadedDocuments().subscribe({
          next: (docs) => {
            this.isLoading = false;
            this.buildSlots(docs);
            this.cdr.markForCheck();
          },
          error: (err) => {
            this.isLoading = false;
            this.loadError = this.errorService.parseError(err);
            this.cdr.markForCheck();
          },
        });
      },
      error: (err) => {
        this.isLoading = false;
        this.loadError = this.errorService.parseError(err);
        this.cdr.markForCheck();
      },
    });
  }

  private buildSlots(uploadedDocs: DocumentData[]): void {
    // Index uploaded docs by documentTypeId. Coerce to Number — the document-type
    // API returns ids as strings while the documents API returns them as numbers.
    const docByTypeId = new Map<number, DocumentData>();
    for (const doc of uploadedDocs) {
      const key = doc.documentTypeId != null ? Number(doc.documentTypeId) : null;
      if (key != null) {
        docByTypeId.set(key, doc);
      }
    }

    // Build a slot per document type
    this.slots = this.allDocumentTypes.map((type) => {
      const uploaded = docByTypeId.get(Number(type.id));
      if (uploaded) {
        return {
          typeId: type.id,
          typeName: type.name,
          isRequired: type.isRequired ?? false,
          documentId: uploaded.id,
          documentName: uploaded.documentName,
          status: (uploaded.documentStatus as DocumentSlot['status']) ?? 'PENDING',
          uploadDate: uploaded.uploadDate ?? null,
          fileSize: uploaded.fileSize ? this.formatFileSize(uploaded.fileSize) : null,
        } as DocumentSlot;
      }
      return {
        typeId: type.id,
        typeName: type.name,
        isRequired: type.isRequired ?? false,
        documentId: null,
        documentName: null,
        status: 'NOT_UPLOADED',
        uploadDate: null,
        fileSize: null,
      } as DocumentSlot;
    });
  }

  retryLoad(): void {
    this.loadPack();
  }

  /** Open the upload modal targeting a specific slot (upload or replace). */
  openUploadModal(slot: DocumentSlot): void {
    this.activeSlot = slot;
    this.showUploadModal = true;
    this.uploadForm.patchValue({ documentType: slot.typeId });
    this.selectedFile = null;
    this.uploadError = null;
    this.fileValidationErrors = [];
    this.uploadProgress = 0;
    this.uploadStatus = 'idle';
  }

  closeUploadModal(): void {
    this.showUploadModal = false;
    this.uploadForm.reset();
    this.selectedFile = null;
    this.activeSlot = null;
    this.uploadError = null;
    this.fileValidationErrors = [];
    this.uploadProgress = 0;
    this.uploadStatus = 'idle';
  }

  dismissUploadError(): void {
    this.uploadError = null;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0];
    if (file) {
      this.uploadStatus = 'validating';
      this.fileValidationErrors = [];
      this.uploadError = null;

      const validation = this.documentService.validateFile(file);
      if (!validation.valid) {
        this.fileValidationErrors = validation.errors;
        this.selectedFile = null;
        this.uploadStatus = 'error';
        return;
      }

      this.selectedFile = file;
      this.uploadStatus = 'idle';
    }
  }

  uploadDocument(): void {
    if (!this.uploadForm.valid || !this.selectedFile) return;

    this.isUploading = true;
    this.uploadError = null;
    this.fileValidationErrors = [];
    this.uploadProgress = 0;
    this.uploadStatus = 'uploading';

    const selectedTypeId: number = this.uploadForm.get('documentType')?.value;

    this.documentService
      .uploadSingleDocumentWithProgress(this.selectedFile, selectedTypeId)
      .subscribe({
        next: (progress) => {
          this.uploadProgress = progress.progress;
          if (progress.status === 'complete') {
            this.isUploading = false;
            this.uploadStatus = 'complete';
            this.errorService.showSuccess('Document uploaded successfully!');
            setTimeout(() => {
              this.closeUploadModal();
              this.loadPack();
            }, 500);
          }
        },
        error: (error) => {
          this.isUploading = false;
          this.uploadStatus = 'error';
          this.uploadProgress = 0;
          this.uploadError = this.errorService.parseError(error);
          this.cdr.markForCheck();
        },
      });
  }

  retryUpload(): void {
    this.uploadDocument();
  }

  deleteDocument(slot: DocumentSlot): void {
    if (!slot.documentId) return;
    confirmAction(this.dialog, {
      title: 'Delete this document?',
      message: `Your "${slot.typeName}" upload is removed. You can upload it again at any time.`,
      confirmLabel: 'Delete',
      destructive: true,
    }).subscribe((confirmed) => {
      if (!confirmed) return;
      this.documentService.deleteDocument(slot.documentId!).subscribe({
        next: () => {
          this.errorService.showSuccess('Document deleted.');
          this.loadPack();
        },
        error: (err) => {
          this.errorService.showErrorSnackbar(this.errorService.parseError(err));
          this.cdr.markForCheck();
        },
      });
    });
  }

  viewDocument(slot: DocumentSlot): void {
    if (!slot.documentId) return;
    this.selectedDocument = {
      id: slot.documentId,
      name: slot.documentName ?? 'Document',
      type: slot.typeName,
      status: slot.status,
    };
  }

  closeViewer(): void {
    this.selectedDocument = null;
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  get uploadedCount(): number {
    return this.slots.filter((s) => s.status !== 'NOT_UPLOADED').length;
  }

  get requiredMissingCount(): number {
    return this.slots.filter((s) => s.isRequired && s.status === 'NOT_UPLOADED').length;
  }
}
