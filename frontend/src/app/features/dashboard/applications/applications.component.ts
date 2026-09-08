import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin, of, Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { ApplicationService } from '../../../core/services/application.service';
import { Application } from '../../../core/models/application.model';
import { DocumentService } from '../../../core/services/document.service';
import { DocumentData } from '../../../core/models/document.model';
import { DocumentTypeService } from '../../../core/services/document-type.service';
import { DocumentType } from '../../../core/models/document-type.model';
import { ErrorHandlingService } from '../../../core/services/error-handling.service';

// Material imports
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

// Shared Components
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { User } from '../../../core/models/user.model';

export interface DocRequirementStatus {
  typeId: number;
  typeName: string;
  uploaded: boolean;
}

@Component({
  selector: 'app-applications',
  templateUrl: './applications.component.html',
  styleUrls: ['./applications.component.scss'],
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    ModernStatusBadgeComponent,
    SkeletonLoaderComponent,
    EmptyStateComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationsComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly applicationService = inject(ApplicationService);
  private readonly documentService = inject(DocumentService);
  private readonly documentTypeService = inject(DocumentTypeService);
  private readonly errorService = inject(ErrorHandlingService);
  private readonly cdr = inject(ChangeDetectorRef);

  currentUser: User | null = null;
  applications: Application[] = [];
  isLoading = false;

  /** Map from applicationId → per-tenant doc requirement status */
  docRequirements: Map<number, DocRequirementStatus[]> = new Map();

  // Material table
  readonly displayedColumns: readonly string[] = [
    'applicationId',
    'year',
    'status',
    'documents',
    'submittedDate',
    'actions',
  ] as const;
  dataSource = new MatTableDataSource<Application>(this.applications);

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    this.loadApplications();
  }

  loadApplications(): void {
    this.isLoading = true;
    this.applicationService.getMyApplications().subscribe({
      next: (applications) => {
        // Sort by date descending (newest first)
        this.applications = applications.sort((a, b) => {
          const dateA = a.createdAt
            ? new Date(a.createdAt).getTime()
            : a.applicationDate
            ? new Date(a.applicationDate).getTime()
            : 0;
          const dateB = b.createdAt
            ? new Date(b.createdAt).getTime()
            : b.applicationDate
            ? new Date(b.applicationDate).getTime()
            : 0;
          return dateB - dateA;
        });
        this.dataSource.data = this.applications;
        this.isLoading = false;
        this.cdr.markForCheck();
        this.loadDocumentRequirements(applications);
      },
      error: (err) => {
        console.error('Failed to fetch applications:', err);
        this.errorService.showWarning('Could not load your applications. Please refresh the page.');
        this.applications = [];
        this.dataSource.data = [];
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  /** Loads required doc types per tenant and student's uploaded doc names, then builds the requirements map. */
  private loadDocumentRequirements(applications: Application[]): void {
    const uniqueTenantIds = [
      ...new Set(applications.map((a) => a.tenantId).filter((id): id is number => id != null)),
    ];
    if (uniqueTenantIds.length === 0) return;

    const requiredByTenant$ = uniqueTenantIds.reduce((acc, tenantId) => {
      acc[tenantId] = this.documentTypeService
        .getRequiredDocumentTypes(tenantId)
        .pipe(catchError(() => of([] as DocumentType[])));
      return acc;
    }, {} as Record<number, Observable<DocumentType[]>>);

    const myDocs$ = this.documentService
      .getUploadedDocuments()
      .pipe(catchError(() => of([] as DocumentData[])));

    forkJoin({ requiredByTenant: forkJoin(requiredByTenant$), myDocs: myDocs$ })
      .pipe(
        map(({ requiredByTenant, myDocs }) => {
          const uploadedNames = new Set(
            (myDocs as DocumentData[]).map((d) => d.documentTypeName?.toLowerCase())
          );
          const result = new Map<number, DocRequirementStatus[]>();
          for (const app of applications) {
            if (app.tenantId == null) continue;
            const required: DocumentType[] =
              (requiredByTenant as Record<number, DocumentType[]>)[app.tenantId] ?? [];
            if (required.length === 0) continue;
            result.set(
              app.id,
              required.map((rt) => ({
                typeId: rt.id,
                typeName: rt.name,
                uploaded: uploadedNames.has(rt.name?.toLowerCase()),
              }))
            );
          }
          return result;
        })
      )
      .subscribe((reqMap) => {
        this.docRequirements = reqMap;
        this.cdr.markForCheck();
      });
  }

  getDocRequirements(appId: number): DocRequirementStatus[] {
    return this.docRequirements.get(appId) ?? [];
  }

  getMissingCount(appId: number): number {
    return this.getDocRequirements(appId).filter((r) => !r.uploaded).length;
  }

  getStatusClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'approved':
        return 'status-approved';
      case 'pending':
        return 'status-pending';
      case 'rejected':
        return 'status-rejected';
      default:
        return 'status-default';
    }
  }

  cancelApplication(_applicationId: number) {
    // Note: Application cancellation is not currently supported by the backend API
    // This is a placeholder for future implementation
    this.errorService.showInfo(
      'Cancelling an application is not available yet. Contact support if you need to withdraw.',
      6000
    );
  }
}
