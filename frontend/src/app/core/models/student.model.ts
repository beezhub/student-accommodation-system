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
  institutionId: number;
  yearOfStudyId: number;
  userId: number;
  // AppUser fields
  firstName: string;
  lastName: string;
  email: string;
}
