import {
  Component,
  Input,
  EventEmitter,
  Output,
  OnInit,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DocumentService } from '../../../../core/services/document.service';
import { ModernStatusBadgeComponent } from '../../../../shared/modern-status-badge/modern-status-badge.component';

@Component({
  selector: 'app-document-viewer',
  templateUrl: './document-viewer.component.html',
  styleUrls: ['./document-viewer.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    ModernStatusBadgeComponent,
  ],
})
export class DocumentViewerComponent implements OnInit {
  private documentService = inject(DocumentService);
  private sanitizer = inject(DomSanitizer);
  private snackBar = inject(MatSnackBar);

  @Input() document: any;
  @Output() viewerClosed = new EventEmitter<void>();

  // eslint-disable-next-line @typescript-eslint/no-explicit-any

  previewUrl: SafeResourceUrl | null = null;
  isLoadingPreview = false;
  previewError = false;
  fileType: 'pdf' | 'image' | 'unknown' = 'unknown';

  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit() {
    this.loadDocumentPreview();
  }

  loadDocumentPreview() {
    if (!this.document?.id) {
      console.error('No document ID provided');
      this.previewError = true;
      this.snackBar.open('This document could not be found.', 'Close', {
        duration: 4000,
        panelClass: ['error-snackbar'],
      });
      return;
    }

    console.log('Loading preview for document:', this.document);
    this.isLoadingPreview = true;
    this.previewError = false;

    this.documentService.downloadDocument(this.document.id).subscribe({
      next: (blob: Blob) => {
        console.log('Document blob received:', blob.type, blob.size);

        // Create object URL for the blob
        const objectUrl = URL.createObjectURL(blob);

        // Determine file type from blob type or filename
        const fileName = this.document.name?.toLowerCase() || '';

        if (blob.type.includes('pdf') || fileName.endsWith('.pdf')) {
          this.fileType = 'pdf';
          this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl);
        } else if (blob.type.includes('image') || fileName.match(/\.(jpg|jpeg|png|gif|webp)$/)) {
          this.fileType = 'image';
          this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl);
        } else {
          this.fileType = 'unknown';
          console.warn('Unknown file type:', blob.type, fileName);
        }

        console.log('Preview loaded successfully. File type:', this.fileType);
        this.isLoadingPreview = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Failed to load document preview:', error);
        console.error('Error details:', {
          status: error.status,
          message: error.message,
          error: error.error,
        });
        this.isLoadingPreview = false;
        this.previewError = true;
        this.snackBar.open('Failed to load document preview. Please try again.', 'Close', {
          duration: 4000,
          panelClass: ['error-snackbar'],
        });
        this.cdr.markForCheck();
      },
    });
  }

  downloadDocument() {
    if (!this.document?.id) return;

    this.documentService.downloadDocument(this.document.id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = this.document.name || 'document';
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Failed to download document:', error);
        this.snackBar.open('Failed to download document. Please try again.', 'Close', {
          duration: 4000,
          panelClass: ['error-snackbar'],
        });
      },
    });
  }

  onClose() {
    // Clean up object URL
    if (this.previewUrl) {
      const url = (this.previewUrl as any).changingThisBreaksApplicationSecurity;
      if (url && url.startsWith('blob:')) {
        URL.revokeObjectURL(url);
      }
    }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    this.viewerClosed.emit();
  }
}
