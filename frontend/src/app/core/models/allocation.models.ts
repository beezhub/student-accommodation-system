export interface DocumentComment {
  id: number;
  documentId: number;
  adminName: string;
  commentText: string;
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
  status: 'PROPOSED' | 'CONFIRMED' | 'REJECTED';
  score: number;
  warnings: string[];
  batchId: string;
  createdAt: Date;
  // Student fields
  studentId: number;
  studentFirstName: string;
  studentLastName: string;
  studentEmail?: string;
  studentGender?: string;
  // Room fields
  roomId: number;
  roomNumber: string;
  roomCapacity?: number;
  currentOccupancy?: number;
  // Building fields
  buildingId?: number;
  buildingName: string;
  // Application fields
  applicationId?: number;
  applicationCode?: string;
  applicationStatus?: string;
  // Proposed by
  proposedById?: number;
  proposedByName?: string;
}

export interface FailedProposal {
  studentId: number;
  studentName: string;
  reason: string;
}

export interface BatchAllocationProposal {
  batchId: string;
  proposals: StagedAllocation[];
  warnings: string[];
  failures: FailedProposal[];
}

/**
 * Answer to confirm-all. It is HTTP 200 even when some rows failed, so read the counts.
 *
 * <p>confirmedAllocationIds holds the ids of the new room allocations, not of the staged rows, so
 * it cannot say which row on the review screen was confirmed.
 */
export interface BatchConfirmationResult {
  success: boolean;
  totalAttempted: number;
  successCount: number;
  failureCount: number;
  errors?: string[];
  confirmedAllocationIds?: number[];
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
