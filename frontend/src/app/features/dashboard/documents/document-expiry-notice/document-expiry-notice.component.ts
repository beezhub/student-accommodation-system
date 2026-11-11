import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  inject,
} from '@angular/core';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DocumentService } from '../../../../core/services/document.service';
import { DocumentExpiry } from '../../../../core/models/document.model';

/**
 * One date and one action per expiring document on the documents page (GH-498): "Your proof of
 * registration expires on 30 November. Upload the new one." Shown only once the new one can be
 * uploaded, that is when the document has expired or expires within the reminder period.
 */
@Component({
  selector: 'app-document-expiry-notice',
  templateUrl: './document-expiry-notice.component.html',
  styleUrls: ['./document-expiry-notice.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, LowerCasePipe, MatButtonModule, MatIconModule],
})
export class DocumentExpiryNoticeComponent implements OnChanges {
  private readonly documentService = inject(DocumentService);
  private readonly cdr = inject(ChangeDetectorRef);

  /** The document pack shown on the page; a new pack (after an upload) reloads the notices. */
  @Input() pack: readonly unknown[] = [];
  /** The student chose to upload the new document. */
  @Output() upload = new EventEmitter<DocumentExpiry>();

  expiries: DocumentExpiry[] = [];

  ngOnChanges(): void {
    this.documentService.getMyDocumentExpiries().subscribe({
      next: (expiries) => {
        this.expiries = expiries.filter((expiry) => expiry.dueForReplacement);
        this.cdr.markForCheck();
      },
      error: () => {
        this.expiries = [];
        this.cdr.markForCheck();
      },
    });
  }
}
