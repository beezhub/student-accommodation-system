export interface AcademicYear {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  applicationOpenDate: string;
  applicationCloseDate: string;
  isCurrent: boolean;
  isApplicationWindowOpen: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AcademicYearRequest {
  name: string;
  startDate: string;
  endDate: string;
  applicationOpenDate: string;
  applicationCloseDate: string;
  isCurrent: boolean;
}
