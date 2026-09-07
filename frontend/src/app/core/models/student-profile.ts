export interface StudentProfile {
  phoneNumber: string;
  studentNumber: string;
  institutionId: string;
  yearOfStudyId: string;
  dateOfBirth: string;
  gender?: string;
  specialRequirements?: string;
  // GH-239: who to call if something happens to this student.
  emergencyContactName?: string;
  emergencyContactRelationship?: string;
  emergencyContactPhone?: string;
  emergencyContactEmail?: string;
  // AppUser fields
  firstName: string;
  lastName: string;
  email: string;
}
