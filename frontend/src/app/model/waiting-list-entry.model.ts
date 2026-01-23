export interface WaitingListEntry {
  studentId: number;
  applicationId: number;
  studentName: string;
  priorityScore: number;
  buildingPreference: string;
  createdAt: Date;
  position: number;
}
