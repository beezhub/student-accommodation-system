# Integration Examples

## Quick Start Guide for New Features

This guide provides step-by-step examples for integrating the new notification system, error handling, and activity logs into your application.

---

## 1. Adding Notifications to Navigation Bar

### Example: Update App Navigation Component

```typescript
// app-nav-menu.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { NotificationsComponent } from './shared/notifications/notifications.component'; // NEW

@Component({
  selector: 'app-nav-menu',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    NotificationsComponent  // NEW: Add notification component
  ],
  template: `
    <mat-toolbar color="primary">
      <span>Student Accommodation System</span>
      <span class="spacer"></span>

      <!-- Add notification bell before other toolbar items -->
      <app-notifications></app-notifications>

      <button mat-icon-button [matMenuTriggerFor]="menu">
        <mat-icon>account_circle</mat-icon>
      </button>
    </mat-toolbar>
  `
})
export class AppNavMenuComponent {}
```

### Example: Initialize Notifications on Login

```typescript
// auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { NotificationService } from './notification.service'; // NEW

@Injectable({ providedIn: 'root' })
export class AuthService {
  private currentUserSubject: BehaviorSubject<any>;

  constructor(
    private http: HttpClient,
    private notificationService: NotificationService // NEW
  ) {
    this.currentUserSubject = new BehaviorSubject<any>(
      JSON.parse(localStorage.getItem('currentUser') || 'null')
    );
  }

  login(email: string, password: string): Observable<any> {
    return this.http.post<any>('/api/auth/login', { email, password }).pipe(
      tap(user => {
        localStorage.setItem('currentUser', JSON.stringify(user));
        this.currentUserSubject.next(user);

        // NEW: Start notification polling after successful login
        this.notificationService.startPolling();
      })
    );
  }

  logout(): void {
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);

    // NEW: Stop notification polling on logout
    this.notificationService.stopPolling();
  }
}
```

---

## 2. Using Error Display Component

### Example 1: Network Error Recovery

```typescript
// documents.component.ts
import { Component, OnInit } from '@angular/core';
import { DocumentService } from '../../core/services/document.service';
import { ErrorDisplayComponent } from '../../shared/error-display/error-display.component';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [CommonModule, ErrorDisplayComponent],
  template: `
    <div *ngIf="!isError">
      <div *ngIf="isLoading">Loading...</div>
      <div *ngIf="!isLoading">
        <!-- Your content here -->
      </div>
    </div>

    <app-error-display
      *ngIf="isError"
      [type]="errorType"
      [showRetry]="true"
      (retry)="loadDocuments()">
    </app-error-display>
  `
})
export class DocumentsComponent implements OnInit {
  isLoading = false;
  isError = false;
  errorType: 'network' | 'server' | 'general' = 'general';
  documents: any[] = [];

  constructor(private documentService: DocumentService) {}

  ngOnInit() {
    this.loadDocuments();
  }

  loadDocuments() {
    this.isLoading = true;
    this.isError = false;

    this.documentService.getDocuments().subscribe({
      next: (docs) => {
        this.documents = docs;
        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        this.isError = true;

        // Determine error type based on status
        if (error.status === 0) {
          this.errorType = 'network';
        } else if (error.status >= 500) {
          this.errorType = 'server';
        } else {
          this.errorType = 'general';
        }
      }
    });
  }
}
```

### Example 2: Custom Error Messages

```typescript
// profile.component.ts
import { Component } from '@angular/core';
import { ErrorDisplayComponent } from '../../shared/error-display/error-display.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ErrorDisplayComponent],
  template: `
    <app-error-display
      *ngIf="profileError"
      type="validation"
      title="Profile Update Failed"
      message="Please check your profile information and ensure all required fields are filled correctly."
      [showRetry]="true"
      [showGoBack]="true"
      customAction="Contact Support"
      (retry)="saveProfile()"
      (goBack)="cancelEdit()"
      (customActionClick)="contactSupport()">
    </app-error-display>
  `
})
export class ProfileComponent {
  profileError = false;

  saveProfile() {
    // Retry save logic
  }

  cancelEdit() {
    // Navigate back
  }

  contactSupport() {
    // Open support dialog
  }
}
```

---

## 3. Registering Error Interceptor

### Example: App Configuration (Standalone)

```typescript
// app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors, HTTP_INTERCEPTORS } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { ErrorInterceptor } from './core/interceptors/error.interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(),
    provideAnimations(),
    // NEW: Register error interceptor
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    }
  ]
};
```

### Example: App Module (NgModule)

```typescript
// app.module.ts
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { ErrorInterceptor } from './core/interceptors/error.interceptor';
import { AppComponent } from './app.component';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    HttpClientModule,
    BrowserAnimationsModule,
    MatSnackBarModule  // Required for error interceptor
  ],
  providers: [
    // NEW: Register error interceptor
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
```

---

## 4. Adding Activity Logs to Admin Dashboard

### Example: Update Admin Routes

```typescript
// app.routes.ts or admin.routes.ts
import { Routes } from '@angular/router';
import { AdminGuard } from './core/guards/admin.guard';
import { ActivityLogsComponent } from './features/admin/activity-logs/activity-logs.component';

export const routes: Routes = [
  {
    path: 'admin',
    canActivate: [AdminGuard],
    children: [
      { path: 'dashboard', component: AdminDashboardComponent },
      { path: 'applications', component: ApplicationReviewComponent },
      { path: 'documents', component: DocumentVerificationComponent },
      { path: 'activity-logs', component: ActivityLogsComponent }, // NEW
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];
```

### Example: Add to Admin Navigation Menu

```typescript
// admin-nav.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-admin-nav',
  standalone: true,
  imports: [CommonModule, RouterModule, MatListModule, MatIconModule],
  template: `
    <mat-nav-list>
      <a mat-list-item routerLink="/admin/dashboard" routerLinkActive="active">
        <mat-icon matListItemIcon>dashboard</mat-icon>
        <span matListItemTitle>Dashboard</span>
      </a>

      <a mat-list-item routerLink="/admin/applications" routerLinkActive="active">
        <mat-icon matListItemIcon>description</mat-icon>
        <span matListItemTitle>Applications</span>
      </a>

      <a mat-list-item routerLink="/admin/documents" routerLinkActive="active">
        <mat-icon matListItemIcon>verified_user</mat-icon>
        <span matListItemTitle>Document Verification</span>
      </a>

      <!-- NEW: Activity Logs -->
      <a mat-list-item routerLink="/admin/activity-logs" routerLinkActive="active">
        <mat-icon matListItemIcon>history</mat-icon>
        <span matListItemTitle>Activity Logs</span>
      </a>
    </mat-nav-list>
  `
})
export class AdminNavComponent {}
```

---

## 5. Custom Notification Handling

### Example: Show Notification After Action

```typescript
// document-verification.component.ts
import { Component } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-document-verification',
  template: `<!-- template -->`
})
export class DocumentVerificationComponent {

  constructor(private snackBar: MatSnackBar) {}

  approveDocument(docId: number) {
    this.adminService.updateDocumentStatus(docId, 'APPROVED').subscribe({
      next: (response) => {
        // Show success message
        this.snackBar.open('Document approved successfully', 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['success-snackbar']
        });

        // Backend automatically creates notification
        // Frontend notification polling will fetch it
      },
      error: (error) => {
        // Error interceptor handles this automatically
        // But you can add custom logic if needed
      }
    });
  }
}
```

### Example: Custom Snackbar Styles

```css
/* styles.css */

/* Success Snackbar */
.success-snackbar {
  background: #10b981 !important;
  color: white !important;
}

.success-snackbar .mat-mdc-snack-bar-label {
  color: white !important;
}

/* Error Snackbar (already defined by error interceptor) */
.error-snackbar {
  background: #ef4444 !important;
  color: white !important;
}

.error-snackbar .mat-mdc-snack-bar-label {
  color: white !important;
}

/* Warning Snackbar */
.warning-snackbar {
  background: #f59e0b !important;
  color: white !important;
}

.warning-snackbar .mat-mdc-snack-bar-label {
  color: white !important;
}

/* Info Snackbar */
.info-snackbar {
  background: #3b82f6 !important;
  color: white !important;
}

.info-snackbar .mat-mdc-snack-bar-label {
  color: white !important;
}
```

---

## 6. Silent Error Handling

### Example: Background Requests Without User Notification

```typescript
// notification.service.ts (polling)
import { HttpClient, HttpHeaders } from '@angular/common/http';

fetchNotifications(): Observable<NotificationResponse> {
  // Add silent error header to prevent error snackbar during polling
  const headers = new HttpHeaders().set('X-Silent-Error', 'true');

  return this.http.get<NotificationResponse>(
    `${this.apiUrl}`,
    { headers }
  ).pipe(
    tap(response => {
      this.notificationsSubject.next(response.notifications);
      this.unreadCountSubject.next(response.unreadCount);
    }),
    catchError(error => {
      // Handle error silently - don't show to user
      console.error('Failed to fetch notifications:', error);
      return of({ notifications: [], unreadCount: 0, totalCount: 0 });
    })
  );
}
```

---

## 7. Activity Log Integration Examples

### Example: Log User Actions Automatically

This would be done on the backend, but here's how to trigger it:

```typescript
// admin.service.ts
updateDocumentStatus(documentId: number, status: string): Observable<DocumentResponse> {
  const params = new HttpParams().set('status', status);

  // Backend should automatically log this action
  // Action: UPDATE
  // EntityType: DOCUMENT
  // Description: "Updated document {id} status to {status}"

  return this.http.put<DocumentResponse>(
    `${this.documentApiUrl}/update-status/${documentId}`,
    null,
    { params }
  );
}
```

### Example: View User's Own Activity

```typescript
// student-activity.component.ts
import { Component, OnInit } from '@angular/core';
import { ActivityLogService } from '../../core/services/activity-log.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-student-activity',
  template: `
    <h2>My Activity History</h2>
    <mat-list>
      <mat-list-item *ngFor="let log of myLogs">
        <mat-icon matListItemIcon>{{ getActionIcon(log.action) }}</mat-icon>
        <div matListItemTitle>{{ log.description }}</div>
        <div matListItemLine>{{ log.timestamp | date:'medium' }}</div>
      </mat-list-item>
    </mat-list>
  `
})
export class StudentActivityComponent implements OnInit {
  myLogs: any[] = [];

  constructor(
    private activityLogService: ActivityLogService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    const currentUser = this.authService.currentUserValue;

    this.activityLogService.getActivityLogs({
      userId: currentUser.id,
      page: 0,
      size: 10
    }).subscribe(response => {
      this.myLogs = response.content;
    });
  }

  getActionIcon(action: string): string {
    return this.activityLogService.getActionIcon(action);
  }
}
```

---

## 8. Complete Component Example

### Full Example: Document Viewer with All Features

```typescript
// document-viewer.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { DocumentService } from '../../core/services/document.service';
import { ErrorDisplayComponent } from '../../shared/error-display/error-display.component';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-document-viewer',
  standalone: true,
  imports: [
    CommonModule,
    ErrorDisplayComponent,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <!-- Loading State -->
    <div *ngIf="isLoading" class="loading-container">
      <mat-spinner></mat-spinner>
      <p>Loading document...</p>
    </div>

    <!-- Error State -->
    <app-error-display
      *ngIf="hasError && !isLoading"
      [type]="errorType"
      [title]="errorTitle"
      [message]="errorMessage"
      [showRetry]="true"
      [showGoBack]="true"
      (retry)="loadDocument()"
      (goBack)="goBack()">
    </app-error-display>

    <!-- Success State -->
    <div *ngIf="document && !isLoading && !hasError" class="document-container">
      <iframe [src]="documentUrl" class="document-frame"></iframe>

      <div class="actions">
        <button mat-raised-button color="primary" (click)="downloadDocument()">
          <mat-icon>download</mat-icon>
          Download
        </button>
      </div>
    </div>
  `,
  styles: [`
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 400px;
      gap: 16px;
    }

    .document-container {
      height: calc(100vh - 200px);
      display: flex;
      flex-direction: column;
    }

    .document-frame {
      flex: 1;
      border: none;
      border-radius: 8px;
    }

    .actions {
      padding: 16px;
      text-align: center;
    }
  `]
})
export class DocumentViewerComponent implements OnInit {
  isLoading = false;
  hasError = false;
  errorType: any = 'general';
  errorTitle?: string;
  errorMessage?: string;

  document: any;
  documentUrl: any;

  constructor(
    private route: ActivatedRoute,
    private documentService: DocumentService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    this.loadDocument();
  }

  loadDocument() {
    const docId = this.route.snapshot.paramMap.get('id');
    if (!docId) {
      this.showError('notfound', 'Document Not Found', 'Invalid document ID');
      return;
    }

    this.isLoading = true;
    this.hasError = false;

    this.documentService.getDocumentPreview(docId).subscribe({
      next: (blob) => {
        this.document = { id: docId };
        this.documentUrl = URL.createObjectURL(blob);
        this.isLoading = false;

        // Activity logged automatically by backend
      },
      error: (error) => {
        this.isLoading = false;

        if (error.status === 0) {
          this.showError('network');
        } else if (error.status === 404) {
          this.showError('notfound', 'Document Not Found');
        } else if (error.status === 403) {
          this.showError('unauthorized', 'Access Denied');
        } else {
          this.showError('server');
        }
      }
    });
  }

  downloadDocument() {
    this.documentService.downloadDocument(this.document.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `document-${this.document.id}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);

        this.snackBar.open('Document downloaded successfully', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });

        // Backend logs download action automatically
      },
      error: (error) => {
        // Error interceptor handles the snackbar
        console.error('Download failed:', error);
      }
    });
  }

  showError(
    type: any,
    title?: string,
    message?: string
  ) {
    this.hasError = true;
    this.errorType = type;
    this.errorTitle = title;
    this.errorMessage = message;
  }

  goBack() {
    window.history.back();
  }
}
```

---

## Summary

These integration examples cover:
1. ✅ Adding notifications to navigation
2. ✅ Initializing notification polling on login
3. ✅ Using error display component
4. ✅ Registering error interceptor
5. ✅ Adding activity logs to admin routes
6. ✅ Custom notification and error handling
7. ✅ Silent error handling for background requests
8. ✅ Complete component example with all features

All features are ready to use! Just follow these examples to integrate them into your application.
