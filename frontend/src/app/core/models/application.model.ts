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
  /** An expiring document of the student has expired at this company (GH-498). */
  hasExpiredDocuments?: boolean;
  /** The student had been checked in at this company when they applied (GH-499). */
  isReturning?: boolean;

  // The company, and the intake and year applied for (GH-495)
  companyName?: string | null;
  academicYearId?: number | null;
  academicYearName?: string | null;
  intakeId?: number | null;
  intakeName?: string | null;
  intakeType?: string | null;
  intakeStayStartDate?: string | null;
  intakeStayEndDate?: string | null;
  institutionId?: number | null;
  preferredBuildings?: { buildingId: number; buildingName: string; preferenceOrder: number }[];
}
