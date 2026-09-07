export interface StudentRequest {
  userId?: number;
  /** Optional at profile setup; collected later during application submission */
  studentNumber?: string;
  phoneNumber?: string;
  dateOfBirth?: string;
  institutionId: number;
  yearOfStudyId: number;
  gender?: string;
  specialRequirements?: string;
  // GH-239: who to call if something happens to this student. Optional here — the backend refuses
  // an application without a name and phone, which is the point at which it starts to matter.
  emergencyContactName?: string;
  emergencyContactRelationship?: string;
  emergencyContactPhone?: string;
  emergencyContactEmail?: string;
  // AppUser fields
  firstName: string;
  lastName: string;
  email: string;
}

export interface StudentResponse {
  id: number;
  phoneNumber: string;
  studentNumber: string;
  dateOfBirth: string;
  registrationDate: string;
  gender: string;
  specialRequirements: string;
  emergencyContactName?: string;
  emergencyContactRelationship?: string;
  emergencyContactPhone?: string;
  emergencyContactEmail?: string;
  institutionId: number;
  yearOfStudyId: number;
  userId: number;
  // AppUser fields
  firstName: string;
  lastName: string;
  email: string;
}
