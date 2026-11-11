export type IntakeType = 'ANNUAL' | 'SEMESTER' | 'TRIMESTER';

/** What a student applies for inside a year: its stay dates and application window (GH-494). */
export interface Intake {
  id: number;
  academicYearId: number;
  intakeType: IntakeType;
  name: string;
  stayStartDate: string;
  stayEndDate: string;
  applicationOpenDate: string;
  applicationCloseDate: string;
  isApplicationWindowOpen: boolean;
  /** Set on an edit: applications under this intake's year, which keep their intake. */
  applicationCount?: number;
  /** Set on an edit: allocations under this intake's year, which keep their dates. */
  allocationCount?: number;
}

/**
 * A company's academic year for one institution, or for "All institutions" when
 * `institutionId` is empty. The current year follows the dates; nobody sets it (GH-439).
 */
export interface AcademicYear {
  id: number;
  tenantId?: number;
  institutionId?: number | null;
  institutionName?: string | null;
  name: string;
  startDate: string;
  endDate: string;
  /** Worked out from the intakes' windows; empty when the year has no intakes (GH-496). */
  applicationOpenDate?: string | null;
  applicationCloseDate?: string | null;
  isApplicationWindowOpen: boolean;
  intakes?: Intake[];
  applicationCount?: number;
  allocationCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

/** A year has no window of its own: each intake has one (GH-496). */
export interface AcademicYearRequest {
  institutionId: number | null;
  name: string;
  startDate: string;
  endDate: string;
}

/** "Create year" from a pattern: the intakes and their windows are filled in. */
export interface AcademicYearPatternRequest {
  institutionId: number | null;
  name: string;
  startDate: string;
  endDate: string;
  intakeTypes: IntakeType[];
}

/** An empty window is filled in from the move-in date. */
export interface IntakeRequest {
  intakeType: IntakeType;
  name: string;
  stayStartDate: string;
  stayEndDate: string;
  applicationOpenDate?: string;
  applicationCloseDate?: string;
}
