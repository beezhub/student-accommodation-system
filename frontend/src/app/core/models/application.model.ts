import { ApplicationStatus } from './status.model';

export interface Application {
  id: number;
  applicationCode: string;
  applicationDate: string;
  status: ApplicationStatus | string;
  createdAt?: string;
  updatedAt?: string;

  // Student info fields
  studentId?: number;
  studentNumber?: string;
  studentName?: string;
  studentEmail?: string;

  // Preference fields
  requiresAccessibility?: boolean;
  // Nullable, not just optional: the reactive form emits null for an untouched control,
  // and `any` at the call site hid the mismatch (GH-158).
  specialRequirements?: string | null;
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
  tenantId?: number;
}
