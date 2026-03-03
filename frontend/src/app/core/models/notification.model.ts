export interface Notification {
  id: number;
  userId: number;
  type: NotificationType;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
  data?: NotificationData;
}

export type NotificationType =
  | 'ROOM_ALLOCATION'
  | 'DOCUMENT_APPROVED'
  | 'DOCUMENT_REJECTED'
  | 'APPLICATION_APPROVED'
  | 'APPLICATION_REJECTED'
  | 'PAYMENT_DUE'
  | 'SYSTEM_ANNOUNCEMENT'
  | 'INFO';

export interface NotificationData {
  roomNumber?: string;
  buildingName?: string;
  documentName?: string;
  applicationId?: number;
  dueDate?: string;
  amount?: number;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  [key: string]: any;
}

export interface NotificationResponse {
  notifications: Notification[];
  unreadCount: number;
  totalCount: number;
}
