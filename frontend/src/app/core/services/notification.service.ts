import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, interval, Subscription, of } from 'rxjs';
import { tap, switchMap, startWith, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Notification, NotificationResponse } from '../models/notification.model';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private apiUrl = `${environment.apiUrl}/notifications`;
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);
  private pollingSubscription?: Subscription;

  public notifications$ = this.notificationsSubject.asObservable();
  public unreadCount$ = this.unreadCountSubject.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Start polling for notifications every 30 seconds
   */
  startPolling(): void {
    // Stop any existing polling first
    this.stopPolling();

    // Check if user is authenticated before starting polling
    const token = localStorage.getItem('token');
    if (!token) {
      console.warn('Cannot start notification polling: User not authenticated');
      return;
    }

    this.pollingSubscription = interval(30000) // Poll every 30 seconds
      .pipe(
        startWith(0), // Start immediately
        switchMap(() => this.fetchNotifications())
      )
      .subscribe();
  }

  /**
   * Fetch all notifications for current user
   */
  fetchNotifications(): Observable<NotificationResponse> {
    // Add silent error header to prevent error interceptor from showing snackbar
    const headers = new HttpHeaders().set('X-Silent-Error', 'true');

    return this.http.get<NotificationResponse>(`${this.apiUrl}`, { headers }).pipe(
      tap((response) => {
        this.notificationsSubject.next(response.notifications);
        this.unreadCountSubject.next(response.unreadCount);
      }),
      catchError((error) => {
        // Silently handle errors - just return empty response
        // This prevents notification errors from breaking the app
        console.warn(
          'Failed to fetch notifications (backend endpoint may not be ready):',
          error.message
        );
        return of({ notifications: [], unreadCount: 0, totalCount: 0 });
      })
    );
  }

  /**
   * Mark a notification as read
   */
  markAsRead(notificationId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${notificationId}/read`, {}).pipe(
      tap(() => {
        const currentNotifications = this.notificationsSubject.value;
        const updatedNotifications = currentNotifications.map((notif) =>
          notif.id === notificationId ? { ...notif, isRead: true } : notif
        );
        this.notificationsSubject.next(updatedNotifications);

        const unreadCount = updatedNotifications.filter((n) => !n.isRead).length;
        this.unreadCountSubject.next(unreadCount);
      })
    );
  }

  /**
   * Mark all notifications as read
   */
  markAllAsRead(): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/read-all`, {}).pipe(
      tap(() => {
        const currentNotifications = this.notificationsSubject.value;
        const updatedNotifications = currentNotifications.map((notif) => ({
          ...notif,
          isRead: true,
        }));
        this.notificationsSubject.next(updatedNotifications);
        this.unreadCountSubject.next(0);
      })
    );
  }

  /**
   * Delete a notification
   */
  deleteNotification(notificationId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${notificationId}`).pipe(
      tap(() => {
        const currentNotifications = this.notificationsSubject.value;
        const updatedNotifications = currentNotifications.filter(
          (notif) => notif.id !== notificationId
        );
        this.notificationsSubject.next(updatedNotifications);

        const unreadCount = updatedNotifications.filter((n) => !n.isRead).length;
        this.unreadCountSubject.next(unreadCount);
      })
    );
  }

  /**
   * Get notification icon based on type
   */
  getNotificationIcon(type: string): string {
    const iconMap: { [key: string]: string } = {
      ROOM_ALLOCATION: 'meeting_room',
      DOCUMENT_APPROVED: 'verified',
      DOCUMENT_REJECTED: 'cancel',
      APPLICATION_APPROVED: 'check_circle',
      APPLICATION_REJECTED: 'error',
      PAYMENT_DUE: 'payment',
      SYSTEM_ANNOUNCEMENT: 'campaign',
      INFO: 'info',
    };
    return iconMap[type] || 'notifications';
  }

  /**
   * Get notification color class based on type
   */
  getNotificationColor(type: string): string {
    const colorMap: { [key: string]: string } = {
      ROOM_ALLOCATION: 'success',
      DOCUMENT_APPROVED: 'success',
      DOCUMENT_REJECTED: 'warn',
      APPLICATION_APPROVED: 'success',
      APPLICATION_REJECTED: 'warn',
      PAYMENT_DUE: 'accent',
      SYSTEM_ANNOUNCEMENT: 'primary',
      INFO: 'primary',
    };
    return colorMap[type] || 'primary';
  }

  /**
   * Stop polling (call on logout)
   */
  stopPolling(): void {
    // Unsubscribe from polling
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.pollingSubscription = undefined;
    }

    // Clear notifications
    this.notificationsSubject.next([]);
    this.unreadCountSubject.next(0);
  }
}
