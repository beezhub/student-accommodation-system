import { Injectable, inject, NgZone } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, interval, Subscription, of } from 'rxjs';
import { tap, switchMap, startWith, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Notification, NotificationResponse } from '../models/notification.model';
import { TokenStorageService } from './token-storage.service';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/notifications`;
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);
  private pollingSubscription?: Subscription;
  private eventSource?: EventSource;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 3;
  private tokenStorage = inject(TokenStorageService);
  private ngZone = inject(NgZone);

  public notifications$ = this.notificationsSubject.asObservable();
  public unreadCount$ = this.unreadCountSubject.asObservable();

  /**
   * Initialize notifications (try SSE, fallback to polling)
   */
  initializeNotifications(): void {
    // Stop any existing connection/polling
    this.stopNotifications();

    // Check if user is authenticated
    const token = this.tokenStorage.getToken();
    if (!token) {
      console.warn('Cannot start notifications: User not authenticated');
      return;
    }

    this.connectToSse();
  }

  /**
   * Connect to Server-Sent Events endpoint.
   * Note: SSE does not support custom headers, so we pass the token as a query param.
   * For production, consider a ticket-based approach (exchange JWT for short-lived opaque ticket).
   */
  private connectToSse(_token?: string): void {
    const freshToken = this.tokenStorage.getToken();
    if (!freshToken) {
      console.warn('Cannot connect SSE: no valid token');
      return;
    }
    const url = `${this.apiUrl}/stream?token=${freshToken}`;

    this.eventSource = new EventSource(url);

    this.eventSource.onopen = () => {
      console.log('SSE connection opened');
      this.reconnectAttempts = 0;
      // Load existing notifications immediately after connection is established
      this.fetchNotifications().subscribe();
    };

    this.eventSource.addEventListener('notification', (event: MessageEvent) => {
      this.ngZone.run(() => {
        try {
          const notification = JSON.parse(event.data);
          this.handleNewNotification(notification);
        } catch (e) {
          console.error('Error parsing SSE notification', e);
        }
      });
    });

    this.eventSource.addEventListener('connected', () => {
      console.log('SSE connected confirmed by server');
    });

    this.eventSource.onerror = (error) => {
      this.ngZone.run(() => {
        console.error('SSE error:', error);
        this.eventSource?.close();

        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++;
          console.log(
            `Retrying SSE connection in 5s (Attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})...`
          );
          setTimeout(() => this.connectToSse(), 5000);
        } else {
          console.warn('Max SSE reconnect attempts reached. Falling back to polling.');
          this.startPolling();
        }
      });
    };
  }

  /**
   * Handle incoming notification from SSE
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private handleNewNotification(_notification: any): void {
    // Refresh full list to keep sync (simplest approach for now)
    // Or optimize by appending to list
    this.fetchNotifications().subscribe();
  }

  /**
   * Fallback: Start polling for notifications every 30 seconds
   */
  private startPolling(): void {
    if (this.pollingSubscription) {
      return;
    }

    console.log('Starting notification polling fallback');
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
        console.warn('Failed to fetch notifications:', error.message);
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
      ALLOCATION_CONFIRMED: 'home',
      ALLOCATION_PROPOSED: 'pending_actions',
      ALLOCATION_REJECTED: 'block',
      CHECK_IN_CONFIRMED: 'how_to_reg',
      CHECK_IN_EXPIRED: 'event_busy',
      CHECK_IN_REMINDER: 'event',
      OFFER_EXPIRED: 'timer_off',
      OFFER_EXPIRY_REMINDER: 'timer',
      ROOM_MOVED: 'swap_horiz',
      WAITING_LIST: 'queue',
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
      ALLOCATION_CONFIRMED: 'success',
      ALLOCATION_PROPOSED: 'primary',
      ALLOCATION_REJECTED: 'warn',
      CHECK_IN_CONFIRMED: 'success',
      CHECK_IN_EXPIRED: 'warn',
      CHECK_IN_REMINDER: 'accent',
      OFFER_EXPIRED: 'warn',
      OFFER_EXPIRY_REMINDER: 'accent',
      ROOM_MOVED: 'primary',
      WAITING_LIST: 'accent',
    };
    return colorMap[type] || 'primary';
  }

  /**
   * Stop notifications (call on logout)
   */
  stopNotifications(): void {
    // Unsubscribe from polling
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.pollingSubscription = undefined;
    }

    // Close SSE connection
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = undefined;
    }

    // Clear notifications
    this.notificationsSubject.next([]);
    this.unreadCountSubject.next(0);
  }
}
