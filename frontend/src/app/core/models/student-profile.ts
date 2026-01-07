export interface StudentProfile {
  phoneNumber: string;
  studentNumber: string;
  institutionId: string;
  yearOfStudyId: string;
  dateOfBirth: string;
  gender?: string;
  specialRequirements?: string;
  // AppUser fields
  firstName: string;
  lastName: string;
  email: string;
}
