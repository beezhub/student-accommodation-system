import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NotificationService } from './notification.service';
import { TokenStorageService } from './token-storage.service';
import { environment } from '../../../environments/environment';
import { Notification, NotificationResponse } from '../models/notification.model';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;
  let tokenStorageSpy: jasmine.SpyObj<TokenStorageService>;

  const apiUrl = `${environment.apiUrl}/notifications`;

  const mockNotification: Notification = {
    id: 1,
    userId: 10,
    type: 'INFO',
    title: 'Test',
    message: 'Hello',
    isRead: false,
    createdAt: '2026-01-01',
  };

  const mockResponse: NotificationResponse = {
    notifications: [mockNotification],
    unreadCount: 1,
    totalCount: 1,
  };

  beforeEach(() => {
    tokenStorageSpy = jasmine.createSpyObj<TokenStorageService>('TokenStorageService', [
      'getToken',
      'isTokenValid',
    ]);
    tokenStorageSpy.getToken.and.returnValue(null);
    tokenStorageSpy.isTokenValid.and.returnValue(false);

    // Stub EventSource so SSE connection attempts don't fail in jsdom
    (window as any).EventSource = jasmine.createSpy('EventSource').and.returnValue({
      addEventListener: jasmine.createSpy(),
      close: jasmine.createSpy(),
      onopen: null,
      onerror: null,
    });

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        NotificationService,
        { provide: TokenStorageService, useValue: tokenStorageSpy },
      ],
    });

    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    service.stopNotifications();
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('fetchNotifications', () => {
    it('fetchNotifications_success_updatesNotificationsSubject', () => {
      let notifs: Notification[] | undefined;
      service.notifications$.subscribe((n) => (notifs = n));

      service.fetchNotifications().subscribe();

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);

      expect(notifs).toEqual([mockNotification]);
    });

    it('fetchNotifications_success_updatesUnreadCount', () => {
      let count: number | undefined;
      service.unreadCount$.subscribe((c) => (count = c));

      service.fetchNotifications().subscribe();

      const req = httpMock.expectOne(apiUrl);
      req.flush(mockResponse);

      expect(count).toBe(1);
    });

    it('fetchNotifications_httpError_returnsEmptyResponse', () => {
      let result: NotificationResponse | undefined;
      service.fetchNotifications().subscribe((r) => (result = r));

      const req = httpMock.expectOne(apiUrl);
      req.error(new ProgressEvent('Network error'));

      expect(result).toEqual({ notifications: [], unreadCount: 0, totalCount: 0 });
    });

    it('fetchNotifications_setsXSilentErrorHeader', () => {
      service.fetchNotifications().subscribe();

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.headers.get('X-Silent-Error')).toBe('true');
      req.flush(mockResponse);
    });
  });

  describe('markAsRead', () => {
    it('markAsRead_validId_putsToReadEndpoint', () => {
      // Pre-load notifications
      service['notificationsSubject'].next([mockNotification]);

      service.markAsRead(1).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/1/read`);
      expect(req.request.method).toBe('PUT');
      req.flush(null);
    });

    it('markAsRead_success_updatesLocalNotificationState', () => {
      service['notificationsSubject'].next([mockNotification]);
      let notifs: Notification[] | undefined;
      service.notifications$.subscribe((n) => (notifs = n));

      service.markAsRead(1).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/1/read`);
      req.flush(null);

      expect(notifs?.find((n) => n.id === 1)?.isRead).toBeTrue();
    });

    it('markAsRead_success_decrementsUnreadCount', () => {
      service['notificationsSubject'].next([mockNotification]);
      service['unreadCountSubject'].next(1);
      let count: number | undefined;
      service.unreadCount$.subscribe((c) => (count = c));

      service.markAsRead(1).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/1/read`);
      req.flush(null);

      expect(count).toBe(0);
    });
  });

  describe('markAllAsRead', () => {
    it('markAllAsRead_putsToReadAllEndpoint', () => {
      service.markAllAsRead().subscribe();

      const req = httpMock.expectOne(`${apiUrl}/read-all`);
      expect(req.request.method).toBe('PUT');
      req.flush(null);
    });

    it('markAllAsRead_success_setsAllNotificationsAsRead', () => {
      const unread: Notification[] = [
        { ...mockNotification, id: 1, isRead: false },
        { ...mockNotification, id: 2, isRead: false },
      ];
      service['notificationsSubject'].next(unread);
      let notifs: Notification[] | undefined;
      service.notifications$.subscribe((n) => (notifs = n));

      service.markAllAsRead().subscribe();
      const req = httpMock.expectOne(`${apiUrl}/read-all`);
      req.flush(null);

      expect(notifs?.every((n) => n.isRead)).toBeTrue();
    });

    it('markAllAsRead_success_setsUnreadCountToZero', () => {
      service['unreadCountSubject'].next(5);
      let count: number | undefined;
      service.unreadCount$.subscribe((c) => (count = c));

      service.markAllAsRead().subscribe();
      const req = httpMock.expectOne(`${apiUrl}/read-all`);
      req.flush(null);

      expect(count).toBe(0);
    });
  });

  describe('stopNotifications', () => {
    it('stopNotifications_clearsNotificationsSubject', () => {
      service['notificationsSubject'].next([mockNotification]);
      let notifs: Notification[] | undefined;
      service.notifications$.subscribe((n) => (notifs = n));

      service.stopNotifications();

      expect(notifs).toEqual([]);
    });

    it('stopNotifications_setsUnreadCountToZero', () => {
      service['unreadCountSubject'].next(3);
      let count: number | undefined;
      service.unreadCount$.subscribe((c) => (count = c));

      service.stopNotifications();

      expect(count).toBe(0);
    });

    it('stopNotifications_closesEventSourceIfOpen', () => {
      const mockEventSource = {
        addEventListener: jasmine.createSpy(),
        close: jasmine.createSpy(),
        onopen: null as any,
        onerror: null as any,
      };
      service['eventSource'] = mockEventSource as any;

      service.stopNotifications();

      expect(mockEventSource.close).toHaveBeenCalled();
      expect(service['eventSource']).toBeUndefined();
    });
  });

  describe('initializeNotifications', () => {
    it('initializeNotifications_noToken_doesNotConnectToSse', () => {
      tokenStorageSpy.getToken.and.returnValue(null);
      service.initializeNotifications();
      expect((window as any).EventSource).not.toHaveBeenCalled();
    });

    it('initializeNotifications_withToken_connectsToSse', () => {
      tokenStorageSpy.getToken.and.returnValue('my-token');
      service.initializeNotifications();
      expect((window as any).EventSource).toHaveBeenCalled();
    });
  });

  describe('deleteNotification', () => {
    it('deleteNotification_validId_deletesToNotificationEndpoint', () => {
      service.deleteNotification(1).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('deleteNotification_success_removesNotificationFromSubject', () => {
      service['notificationsSubject'].next([mockNotification, { ...mockNotification, id: 2, isRead: true }]);
      let notifs: Notification[] | undefined;
      service.notifications$.subscribe((n) => (notifs = n));

      service.deleteNotification(1).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/1`);
      req.flush(null);

      expect(notifs?.length).toBe(1);
      expect(notifs?.[0].id).toBe(2);
    });

    it('deleteNotification_success_updatesUnreadCount', () => {
      service['notificationsSubject'].next([mockNotification]);
      service['unreadCountSubject'].next(1);
      let count: number | undefined;
      service.unreadCount$.subscribe((c) => (count = c));

      service.deleteNotification(1).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/1`);
      req.flush(null);

      expect(count).toBe(0);
    });
  });

  describe('getNotificationIcon', () => {
    it('getNotificationIcon_roomAllocation_returnsMeetingRoom', () => {
      expect(service.getNotificationIcon('ROOM_ALLOCATION')).toBe('meeting_room');
    });

    it('getNotificationIcon_documentApproved_returnsVerified', () => {
      expect(service.getNotificationIcon('DOCUMENT_APPROVED')).toBe('verified');
    });

    it('getNotificationIcon_documentRejected_returnsCancel', () => {
      expect(service.getNotificationIcon('DOCUMENT_REJECTED')).toBe('cancel');
    });

    it('getNotificationIcon_applicationApproved_returnsCheckCircle', () => {
      expect(service.getNotificationIcon('APPLICATION_APPROVED')).toBe('check_circle');
    });

    it('getNotificationIcon_applicationRejected_returnsError', () => {
      expect(service.getNotificationIcon('APPLICATION_REJECTED')).toBe('error');
    });

    it('getNotificationIcon_paymentDue_returnsPayment', () => {
      expect(service.getNotificationIcon('PAYMENT_DUE')).toBe('payment');
    });

    it('getNotificationIcon_systemAnnouncement_returnsCampaign', () => {
      expect(service.getNotificationIcon('SYSTEM_ANNOUNCEMENT')).toBe('campaign');
    });

    it('getNotificationIcon_info_returnsInfo', () => {
      expect(service.getNotificationIcon('INFO')).toBe('info');
    });

    it('getNotificationIcon_unknownType_returnsNotificationsDefault', () => {
      expect(service.getNotificationIcon('UNKNOWN')).toBe('notifications');
    });
  });

  describe('getNotificationColor', () => {
    it('getNotificationColor_roomAllocation_returnsSuccess', () => {
      expect(service.getNotificationColor('ROOM_ALLOCATION')).toBe('success');
    });

    it('getNotificationColor_documentApproved_returnsSuccess', () => {
      expect(service.getNotificationColor('DOCUMENT_APPROVED')).toBe('success');
    });

    it('getNotificationColor_documentRejected_returnsWarn', () => {
      expect(service.getNotificationColor('DOCUMENT_REJECTED')).toBe('warn');
    });

    it('getNotificationColor_paymentDue_returnsAccent', () => {
      expect(service.getNotificationColor('PAYMENT_DUE')).toBe('accent');
    });

    it('getNotificationColor_systemAnnouncement_returnsPrimary', () => {
      expect(service.getNotificationColor('SYSTEM_ANNOUNCEMENT')).toBe('primary');
    });

    it('getNotificationColor_unknownType_returnsPrimaryDefault', () => {
      expect(service.getNotificationColor('UNKNOWN')).toBe('primary');
    });
  });
});
