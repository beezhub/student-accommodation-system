export type AllocationActionType = 'MOVE' | 'OVERRIDE' | 'DELETE' | 'FORCE_ALLOCATE';

export interface AllocationAuditLog {
  id: number;
  allocationId: number;
  actionType: AllocationActionType;
  adminUserName: string;
  reason: string;
  oldRoomId?: number;
  newRoomId?: number;
  constraintBypassed?: string;
  createdAt: Date;
}
