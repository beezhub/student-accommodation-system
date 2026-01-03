# Frontend Enhancements Guide

## New Features Implemented

This guide covers the implementation of three major frontend features:
1. **Notification System** - Real-time notifications for users
2. **Enhanced Error Handling** - Global error interceptor and error display components
3. **Activity Log Viewer** - Admin tool for monitoring system activities

---

## 1. Notification System

### Overview
A real-time notification system that displays important updates to users including room allocations, document approvals, application status changes, and system announcements.

### Components Created

#### A. Notification Model (`notification.model.ts`)
```typescript
interface Notification {
  id: number;
  userId: number;
  type: NotificationType;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
  data?: NotificationData;
}
```

**Supported Notification Types:**
- `ROOM_ALLOCATION` - Room assignment notifications
- `DOCUMENT_APPROVED` - Document approval notifications
- `DOCUMENT_REJECTED` - Document rejection notifications
- `APPLICATION_APPROVED` - Application approval notifications
- `APPLICATION_REJECTED` - Application rejection notifications
- `PAYMENT_DUE` - Payment reminder notifications
- `SYSTEM_ANNOUNCEMENT` - System-wide announcements
- `INFO` - General information

#### B. Notification Service (`notification.service.ts`)
**Features:**
- Automatic polling every 30 seconds
- Mark notifications as read
- Mark all as read
- Delete notifications
- Get unread count
- Icon and color mapping for notification types

**Key Methods:**
```typescript
startPolling()                          // Start fetching notifications
fetchNotifications()                     // Get all notifications
markAsRead(notificationId: number)      // Mark one as read
markAllAsRead()                         // Mark all as read
deleteNotification(notificationId)      // Delete notification
stopPolling()                           // Call on logout
```

#### C. Notifications Component (`notifications.component.ts/html/css`)
**Features:**
- Bell icon with unread badge
- Dropdown menu with notification list
- "Mark all as read" action
- Individual notification actions
- Delete functionality
- Time ago display
- Special data display for different notification types
- Responsive design

### Integration Steps

#### Step 1: Add to App Navigation
```typescript
// In app-nav-menu.component.html or navbar component
import { NotificationsComponent } from './shared/notifications/notifications.component';

// Add to component imports
imports: [
  // ... other imports
  NotificationsComponent
]

// In template (add to toolbar/navbar):
<app-notifications></app-notifications>
```

#### Step 2: Initialize on Login
```typescript
// In auth.service.ts or login component
import { NotificationService } from './core/services/notification.service';

constructor(private notificationService: NotificationService) {}

onLogin() {
  // After successful login
  this.notificationService.startPolling();
}

onLogout() {
  // On logout
  this.notificationService.stopPolling();
}
```

#### Step 3: Backend API Requirements

**GET /api/notifications**
```json
{
  "notifications": [
    {
      "id": 1,
      "userId": 123,
      "type": "ROOM_ALLOCATION",
      "title": "Room Allocated",
      "message": "Your room has been allocated!",
      "isRead": false,
      "createdAt": "2024-01-15T10:30:00Z",
      "data": {
        "roomNumber": "A-101",
        "buildingName": "North Building"
      }
    }
  ],
  "unreadCount": 5,
  "totalCount": 20
}
```

**PUT /api/notifications/{id}/read**
```json
// No request body
// Response: 204 No Content or 200 OK
```

**PUT /api/notifications/read-all**
```json
// No request body
// Response: 204 No Content or 200 OK
```

**DELETE /api/notifications/{id}**
```json
// No request body
// Response: 204 No Content
```

---

## 2. Enhanced Error Handling

### Overview
A comprehensive error handling system with global error interception, user-friendly error messages, and retry mechanisms.

### Components Created

#### A. Error Interceptor (`error.interceptor.ts`)
**Features:**
- Automatic retry for GET requests (1 retry)
- Smart error message extraction
- Automatic redirect on 401 (Unauthorized)
- Automatic redirect on 403 (Forbidden)
- Network error detection
- User-friendly error display via snackbar

**Error Handling Flow:**
```
Request Error
  ↓
Retry (if GET request)
  ↓
Extract Error Message
  ↓
Handle Specific Status Codes
  ↓
Display Snackbar
  ↓
Redirect if needed (401, 403)
```

#### B. Error Display Component (`error-display.component.ts`)
**Features:**
- Multiple error types with custom icons
- Retry action
- Go back action
- Custom actions
- Responsive design

**Error Types:**
- `network` - Connection errors
- `server` - Server-side errors
- `validation` - Input validation errors
- `notfound` - 404 errors
- `unauthorized` - 403 errors
- `general` - General errors

**Usage Example:**
```html
<app-error-display
  type="network"
  [showRetry]="true"
  [showGoBack]="false"
  (retry)="onRetry()">
</app-error-display>
```

### Integration Steps

#### Step 1: Register Error Interceptor
```typescript
// In app.config.ts or app.module.ts
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { ErrorInterceptor } from './core/interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    // ... other providers
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    }
  ]
};
```

#### Step 2: Add Global Snackbar Styles
```css
/* In styles.css */
.error-snackbar {
  background: #ef4444 !important;
  color: white !important;
}

.error-snackbar .mat-mdc-snack-bar-label {
  color: white !important;
}
```

#### Step 3: Use Error Display Component
```typescript
// In any component
import { ErrorDisplayComponent } from './shared/error-display/error-display.component';

// Add to imports
imports: [ErrorDisplayComponent]

// In component class
hasError = false;
errorType: ErrorType = 'general';

loadData() {
  this.service.getData().subscribe({
    next: (data) => this.handleData(data),
    error: (error) => {
      this.hasError = true;
      this.errorType = error.status === 0 ? 'network' : 'server';
    }
  });
}

retry() {
  this.hasError = false;
  this.loadData();
}
```

```html
<!-- In template -->
<div *ngIf="!hasError">
  <!-- Normal content -->
</div>

<app-error-display
  *ngIf="hasError"
  [type]="errorType"
  (retry)="retry()">
</app-error-display>
```

#### Step 4: Silent Errors (Optional)
For requests where you don't want to show error snackbars:
```typescript
const headers = new HttpHeaders().set('X-Silent-Error', 'true');
this.http.get(url, { headers }).subscribe(...);
```

---

## 3. Activity Log Viewer

### Overview
An admin tool for viewing and monitoring all system activities including user actions, data changes, and system events.

### Components Created

#### A. Activity Log Model (`activity-log.model.ts`)
```typescript
interface ActivityLog {
  id: number;
  userId: number;
  userEmail: string;
  userFullName: string;
  userRole: string;
  action: ActivityAction;
  entityType: EntityType;
  entityId?: number;
  description: string;
  ipAddress?: string;
  timestamp: string;
  metadata?: object;
}
```

**Activity Actions:**
- `CREATE`, `UPDATE`, `DELETE` - CRUD operations
- `LOGIN`, `LOGOUT` - Authentication
- `APPROVE`, `REJECT` - Approvals
- `UPLOAD`, `DOWNLOAD` - File operations
- `VIEW`, `EXPORT` - Data access

**Entity Types:**
- `APPLICATION`, `DOCUMENT`, `USER`, `ROOM`, `PAYMENT`, `NOTIFICATION`, `SYSTEM`

#### B. Activity Log Service (`activity-log.service.ts`)
**Features:**
- Fetch logs with filters
- Export logs to CSV
- Icon and color mapping

**Key Methods:**
```typescript
getActivityLogs(filters)     // Get filtered logs
exportActivityLogs(filters)  // Export to CSV
```

#### C. Activity Logs Component (`activity-logs.component.ts`)
**Features:**
- Advanced filtering (role, action, entity type, date range)
- Pagination
- Export to CSV
- Color-coded action badges
- User information display
- IP address tracking
- Responsive design

### Integration Steps

#### Step 1: Add Route
```typescript
// In app-routing.module.ts or admin routes
{
  path: 'admin/activity-logs',
  component: ActivityLogsComponent,
  canActivate: [AdminGuard]
}
```

#### Step 2: Add to Admin Menu
```html
<!-- In admin navigation menu -->
<a mat-list-item routerLink="/admin/activity-logs">
  <mat-icon>history</mat-icon>
  <span>Activity Logs</span>
</a>
```

#### Step 3: Backend API Requirements

**GET /api/admin/activity-logs**
```
Query Parameters:
- userId (optional)
- userRole (optional): ADMIN, STUDENT, LANDLORD
- action (optional): CREATE, UPDATE, DELETE, etc.
- entityType (optional): APPLICATION, DOCUMENT, etc.
- startDate (optional): YYYY-MM-DD
- endDate (optional): YYYY-MM-DD
- page (default: 0)
- size (default: 20)

Response:
{
  "content": [
    {
      "id": 1,
      "userId": 123,
      "userEmail": "admin@example.com",
      "userFullName": "Admin User",
      "userRole": "ADMIN",
      "action": "APPROVE",
      "entityType": "DOCUMENT",
      "entityId": 456,
      "description": "Approved student ID document",
      "ipAddress": "192.168.1.1",
      "timestamp": "2024-01-15T10:30:00Z",
      "metadata": {}
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0
}
```

**GET /api/admin/activity-logs/export**
```
Query Parameters: Same as above
Response: CSV file (Content-Type: text/csv)
```

---

## File Structure

```
frontend/src/app/
├── core/
│   ├── interceptors/
│   │   └── error.interceptor.ts                    # NEW
│   ├── models/
│   │   ├── notification.model.ts                   # NEW
│   │   └── activity-log.model.ts                   # NEW
│   └── services/
│       ├── notification.service.ts                  # NEW
│       └── activity-log.service.ts                  # NEW
│
├── shared/
│   ├── notifications/
│   │   ├── notifications.component.ts               # NEW
│   │   ├── notifications.component.html             # NEW
│   │   └── notifications.component.css              # NEW
│   └── error-display/
│       ├── error-display.component.ts               # NEW
│       ├── error-display.component.html             # NEW
│       └── error-display.component.css              # NEW
│
└── features/
    └── admin/
        └── activity-logs/
            ├── activity-logs.component.ts           # NEW
            ├── activity-logs.component.html         # NEW
            └── activity-logs.component.css          # NEW
```

---

## Testing Checklist

### Notification System
- [ ] Notifications load on login
- [ ] Unread count displays correctly
- [ ] Mark as read updates badge
- [ ] Mark all as read clears badge
- [ ] Delete notification works
- [ ] Time ago displays correctly
- [ ] Different notification types display correctly
- [ ] Responsive design works
- [ ] Polling starts on login
- [ ] Polling stops on logout

### Error Handling
- [ ] Global error interceptor catches errors
- [ ] GET requests retry once
- [ ] Error messages display in snackbar
- [ ] 401 redirects to login
- [ ] 403 redirects to unauthorized page
- [ ] Network errors show appropriate message
- [ ] Error display component shows correct icons
- [ ] Retry action works
- [ ] Silent errors don't show snackbar

### Activity Logs
- [ ] Logs load with default filters
- [ ] All filters work correctly
- [ ] Pagination works
- [ ] Export creates CSV file
- [ ] Action badges display with correct colors
- [ ] User information displays correctly
- [ ] Date range filter works
- [ ] Responsive design works
- [ ] Only admins can access

---

## Styling Customization

### Notification Colors
```css
/* In notifications.component.css */
.icon-success { /* Green gradient */ }
.icon-warn { /* Red gradient */ }
.icon-accent { /* Amber gradient */ }
.icon-primary { /* Blue gradient */ }
```

### Error Snackbar
```css
/* In styles.css */
.error-snackbar {
  background: #ef4444 !important; /* Change error color */
}
```

### Activity Log Action Colors
```css
/* In activity-logs.component.css */
.action-success { /* Green for CREATE, APPROVE, LOGIN */ }
.action-primary { /* Blue for UPDATE, UPLOAD */ }
.action-warn { /* Red for DELETE, REJECT */ }
.action-neutral { /* Gray for VIEW, LOGOUT */ }
```

---

## Performance Considerations

1. **Notification Polling**
   - Default: 30 seconds interval
   - To change: Modify `interval(30000)` in notification.service.ts
   - Consider using WebSocket for real-time updates

2. **Error Retry**
   - Only GET requests retry once
   - 1 second delay between retries
   - To change: Modify retry config in error.interceptor.ts

3. **Activity Log Pagination**
   - Default page size: 20
   - Options: 10, 20, 50, 100
   - Large datasets automatically paginated

---

## Security Notes

1. **Authorization**
   - Activity logs accessible only to admins
   - Add `@PreAuthorize("hasRole('ADMIN')")` on backend endpoints

2. **Data Privacy**
   - IP addresses logged for security audit
   - Consider GDPR compliance for activity logs
   - Implement log retention policy

3. **Error Messages**
   - Don't expose sensitive information in error messages
   - Log detailed errors server-side only
   - Show user-friendly messages to clients

---

## Next Steps

1. **Backend Implementation**
   - Create notification endpoints
   - Create activity log endpoints
   - Add activity logging to all important actions

2. **Testing**
   - Write unit tests for all services
   - Write integration tests for components
   - Test error scenarios

3. **Enhancements**
   - Add WebSocket for real-time notifications
   - Add push notifications
   - Add notification preferences
   - Add activity log analytics dashboard

---

## Support

For questions or issues with these features, refer to:
- Component documentation in code comments
- Backend API requirements in this guide
- Testing checklist for validation
