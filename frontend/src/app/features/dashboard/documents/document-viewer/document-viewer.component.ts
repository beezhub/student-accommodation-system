import { Component, Input, EventEmitter, Output, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { DocumentService } from '../../../../core/services/document.service';
import { ModernStatusBadgeComponent } from '../../../../shared/modern-status-badge/modern-status-badge.component';

@Component({
  selector: 'app-document-viewer',
  templateUrl: './document-viewer.component.html',
  styleUrls: ['./document-viewer.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    ModernStatusBadgeComponent,
  ],
})
export class DocumentViewerComponent implements OnInit {
  @Input() document: any;
  @Output() close = new EventEmitter<void>();

  previewUrl: SafeResourceUrl | null = null;
  isLoadingPreview = false;
  previewError = false;
  fileType: 'pdf' | 'image' | 'unknown' = 'unknown';

  constructor(private documentService: DocumentService, private sanitizer: DomSanitizer) {}

  ngOnInit() {
    this.loadDocumentPreview();
  }

  loadDocumentPreview() {
    if (!this.document?.id) {
      this.previewError = true;
      return;
    }

    this.isLoadingPreview = true;
    this.previewError = false;

    this.documentService.getDocumentPreview(this.document.id).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);

        // Determine file type from blob type
        if (blob.type.includes('pdf')) {
          this.fileType = 'pdf';
          this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
        } else if (blob.type.includes('image')) {
          this.fileType = 'image';
          this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
        } else {
          // Try to determine from document name
          const fileName = this.document.name?.toLowerCase() || '';
          if (fileName.endsWith('.pdf')) {
            this.fileType = 'pdf';
            this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
          } else if (fileName.match(/\.(jpg|jpeg|png|gif|webp)$/)) {
            this.fileType = 'image';
            this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
          } else {
            this.fileType = 'unknown';
          }
        }

        this.isLoadingPreview = false;
      },
      error: (error) => {
        console.error('Failed to load document preview:', error);
        this.isLoadingPreview = false;
        this.previewError = true;
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
        alert('Failed to download document. Please try again.');
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
    this.close.emit();
  }
}
