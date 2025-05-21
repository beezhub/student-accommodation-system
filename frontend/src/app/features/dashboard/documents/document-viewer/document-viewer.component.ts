import { Component, Input, EventEmitter,Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-document-viewer',
  templateUrl: './document-viewer.component.html',
  styleUrls: ['./document-viewer.component.css'],
  standalone: true,
  imports: [CommonModule]
})
export class DocumentViewerComponent {
  @Input() document: any;
  @Output() close = new EventEmitter<void>();

  getStatusClass(status: string): string {
    if (!status) return 'bg-secondary';
    switch (status.toLowerCase()) {
      case 'verified':
        return 'bg-success';
      case 'pending':
        return 'bg-warning';
      case 'rejected':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  }

  onClose() {
    this.close.emit();
  }

}