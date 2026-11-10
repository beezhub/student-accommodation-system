/** An intake as the application form shows it (GH-495). Dates are `yyyy-MM-dd` calendar days. */
export interface IntakeOption {
  intakeId: number;
  intakeName: string;
  intakeType?: string;
  academicYearId: number;
  academicYearName: string;
  stayStartDate: string;
  stayEndDate: string;
  applicationOpenDate: string;
  applicationCloseDate: string;
}

/** The student's own active application with a company, linked instead of a new form. */
export interface ExistingApplicationLink {
  applicationId: number;
  applicationCode: string;
  academicYearId?: number | null;
  academicYearName?: string | null;
  intakeName?: string | null;
  status: string;
}

/** What a student can apply for at one company, checked as its buildings are picked. */
export interface CompanyApplyOptions {
  buildingId?: number;
  companyId: number;
  companyName: string;
  openIntakes: IntakeOption[];
  existingApplications: ExistingApplicationLink[];
  nextIntake?: IntakeOption | null;
  missingDocuments: string[];
}

/** The submit request: buildings from any companies and the intake chosen at each company. */
export interface ApplicationSubmission {
  studentId: number;
  requiresAccessibility: boolean;
  specialRequirements: string | null;
  institutionName: string;
  courseName: string;
  preferredBuildingIds: number[];
  /** Intake id by company id. A company left out uses its one open intake. */
  intakeIds: Record<number, number>;
}
