import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, formatDate } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivityLogService } from '../../../core/services/activity-log.service';
import { ActivityLog, ActivityAction, EntityType } from '../../../core/models/activity-log.model';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

/**
 * YYYY-MM-DD in the user's own time. The picker builds a Date at local midnight, and `toISOString`
 * converts to UTC first, so in South Africa it gave the day before (GH-462).
 */
function toLocalIsoDate(date: Date | null | undefined | ''): string | undefined {
  return date ? formatDate(date, 'yyyy-MM-dd', 'en-US') : undefined;
}

@Component({
  selector: 'app-activity-logs',
  templateUrl: './activity-logs.component.html',
  styleUrls: ['./activity-logs.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatChipsModule,
    MatSnackBarModule,
    SkeletonLoaderComponent,
    EmptyStateComponent,
  ],
})
export class ActivityLogsComponent implements OnInit {
  private activityLogService = inject(ActivityLogService);
  private fb = inject(FormBuilder);
  private snackBar = inject(MatSnackBar);

  displayedColumns: string[] = [
    'timestamp',
    'user',
    'action',
    'entityType',
    'description',
    'ipAddress',
  ];

  dataSource = new MatTableDataSource<ActivityLog>([]);
  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  filtersVisible = false;

  filterForm: FormGroup;

  get activeFilterCount(): number {
    const v = this.filterForm?.value ?? {};
    return [v.userRole, v.action, v.entityType, v.startDate, v.endDate].filter(Boolean).length;
  }

  toggleFilters(): void {
    this.filtersVisible = !this.filtersVisible;
  }

  actions: ActivityAction[] = [
    'CREATE',
    'UPDATE',
    'DELETE',
    'LOGIN',
    'LOGOUT',
    'APPROVE',
    'REJECT',
    'UPLOAD',
    'DOWNLOAD',
    'VIEW',
    'EXPORT',
  ];

  entityTypes: EntityType[] = [
    'APPLICATION',
    'DOCUMENT',
    'USER',
    'ROOM',
    'PAYMENT',
    'NOTIFICATION',
    'SYSTEM',
  ];

  userRoles: string[] = [
    'PLATFORM_ADMIN',
    'PLATFORM_STAFF',
    'TENANT_ADMIN',
    'BUILDING_STAFF',
    'STUDENT',
  ];

  constructor() {
    this.filterForm = this.fb.group({
      userRole: [''],
      action: [''],
      entityType: [''],
      startDate: [''],
      endDate: [''],
    });
  }

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.isLoading = true;
    const filters = {
      ...this.filterForm.value,
      startDate: toLocalIsoDate(this.filterForm.value.startDate),
      endDate: toLocalIsoDate(this.filterForm.value.endDate),
      page: this.currentPage,
      size: this.pageSize,
    };

    this.activityLogService.getActivityLogs(filters).subscribe({
      next: (response) => {
        this.dataSource.data = response.content;
        this.totalElements = response.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load activity logs:', error);
        this.snackBar.open('Failed to load activity logs', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onFilter(): void {
    this.currentPage = 0;
    this.loadLogs();
  }

  onClearFilters(): void {
    this.filterForm.reset();
    this.currentPage = 0;
    this.loadLogs();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadLogs();
  }

  exportLogs(): void {
    const filters = {
      ...this.filterForm.value,
      startDate: toLocalIsoDate(this.filterForm.value.startDate),
      endDate: toLocalIsoDate(this.filterForm.value.endDate),
    };

    this.activityLogService.exportActivityLogs(filters).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `activity-logs-${toLocalIsoDate(new Date())}.csv`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.snackBar.open('Activity logs exported successfully', 'Close', { duration: 3000 });
      },
      error: (error) => {
        console.error('Failed to export logs:', error);
        this.snackBar.open('Failed to export activity logs', 'Close', { duration: 3000 });
      },
    });
  }

  getActionIcon(action: string): string {
    return this.activityLogService.getActionIcon(action);
  }

  getActionColor(action: string): string {
    return this.activityLogService.getActionColor(action);
  }

  getFormattedTimestamp(timestamp: string): string {
    return new Date(timestamp).toLocaleString();
  }

  getEntityIcon(entityType: string): string {
    const iconMap: { [key: string]: string } = {
      APPLICATION: 'description',
      DOCUMENT: 'folder',
      USER: 'person',
      ROOM: 'meeting_room',
      PAYMENT: 'payment',
      NOTIFICATION: 'notifications',
      SYSTEM: 'settings',
    };
    return iconMap[entityType] || 'category';
  }
}
