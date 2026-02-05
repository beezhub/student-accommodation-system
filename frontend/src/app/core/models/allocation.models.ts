export interface DocumentComment {
  id: number;
  documentId: number;
  adminName: string;
  commentText: string;
  createdAt: Date;
}

export interface AllocationRule {
  id: number;
  buildingId?: number;
  ruleName: string;
  preferenceMatchWeight: number;
  desirabilityWeight: number;
  accessibilityWeight: number;
  active: boolean;
  priority: number;
  createdAt: Date;
}

export interface BuildingAttributes {
  id: number;
  buildingId: number;
  genderRestriction: 'NONE' | 'MALE_ONLY' | 'FEMALE_ONLY';
  accessibilityFeatures: string;
  desirabilityScore: number; // 1-10
  allowMixedGender: boolean;
}

export interface RoomAttributes {
  id: number;
  roomId: number;
  accessibilityCompliant: boolean;
  genderRestriction: 'NONE' | 'MALE_ONLY' | 'FEMALE_ONLY';
  desirabilityOverride?: number;
}

export interface RoomDetail {
  buildingName: string;
  roomNumber: string;
  floor: number;
  capacity: number;
}

export interface StagedAllocation {
  id: number;
  studentId: number;
  roomId: number;
  applicationId?: number;
  status: 'PROPOSED' | 'CONFIRMED' | 'REJECTED';
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
  proposals: StagedAllocation[];
  warnings: string[];
}

export interface AllocationAuditLog {
  id: number;
  allocationId: number;
  actionType: 'MOVE' | 'OVERRIDE' | 'DELETE' | 'FORCE_ALLOCATE';
  adminUserName: string;
  reason: string;
  oldRoomId?: number;
  newRoomId?: number;
  constraintBypassed?: string;
  createdAt: Date;
}

export interface WaitingListEntry {
  id: number;
  studentId: number;
  studentNumber: string;
  studentName: string;
  studentEmail: string;
  studentGender: string;
  applicationId: number;
  applicationCode: string;
  applicationStatus: string;
  preferredBuildingId: number;
  preferredBuildingName: string;
  priorityScore: number;
  bestRoomScore: number;
  bestMatchRoomId: number;
  bestMatchRoomNumber: string;
  status: string;
  createdAt: Date;
  position: number;
}
