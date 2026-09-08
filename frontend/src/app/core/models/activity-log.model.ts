export interface ActivityLog {
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
  userAgent?: string;
  timestamp: string;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  metadata?: { [key: string]: any };
}

export type ActivityAction =
  | 'CREATE'
  | 'UPDATE'
  | 'DELETE'
  | 'LOGIN'
  | 'LOGOUT'
  | 'APPROVE'
  | 'REJECT'
  | 'UPLOAD'
  | 'DOWNLOAD'
  | 'VIEW'
  | 'EXPORT';

export type EntityType =
  | 'APPLICATION'
  | 'DOCUMENT'
  | 'USER'
  | 'ROOM'
  | 'PAYMENT'
  | 'NOTIFICATION'
  | 'SYSTEM';

export interface ActivityLogFilters {
  userId?: number;
  userRole?: string;
  action?: ActivityAction;
  entityType?: EntityType;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

export interface ActivityLogResponse {
  content: ActivityLog[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
