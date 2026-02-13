export type StagedAllocationStatus = 'PROPOSED' | 'CONFIRMED' | 'REJECTED';

export interface RoomDetail {
  id: number;
  roomNumber: string;
  buildingName: string;
  capacity: number;
  currentOccupancy: number;
}

export interface StagedAllocation {
  id: number;
  studentId: number;
  roomId: number;
  applicationId?: number;
  status: StagedAllocationStatus;
  score: number;
  warnings: string[];
  proposedBy: number;
  batchId: string;
  roomDetails: RoomDetail;
  studentName: string;
  createdAt: Date;
}

export interface BatchAllocationProposal {
  batchId: string;
  proposedCount: number;
  successCount: number;
  failedCount: number;
  allocations: StagedAllocation[];
  errors: string[];
}
