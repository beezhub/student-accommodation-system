export interface Application {
  id: number;
  applicationCode: string;
  applicationDate: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;

  // Student info fields
  studentId?: number;
  studentNumber?: string;
  studentName?: string;
  studentEmail?: string;

  // Preference fields
  preferredAccommodationType?: string;
  preferredMoveInDate?: string;
  preferredDurationMonths?: number;
  requiresAccessibility?: boolean;
  specialRequirements?: string;
  institutionName?: string;
  courseName?: string;

  // Building preference fields
  preferredBuildingId?: number;
  preferredBuildingName?: string;

  // Allocation fields
  allocationMethod?: string;
  remarks?: string;

  // Review status & statistics
  allocationScore?: number;
  documentCompletionPercent?: number;
  approvedDocuments?: number;
  totalRequiredDocuments?: number;
  allocationStatus?: string;
}
