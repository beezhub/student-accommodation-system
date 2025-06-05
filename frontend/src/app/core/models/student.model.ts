export interface StudentRequest {
  userId: number;
  studentNumber: string;
  dateOfBirth: string;
  institutionId: number;
  yearOfStudyId: number;
  gender: string;
  specialRequirements: string;
  phoneNumber: string;
}

export interface StudentResponse {
  id: number;
  phoneNumber: string;
  dateOfBirth: string;
  registrationDate: string;
  gender: string;
  specialRequirements: string;
  institutionId: number;
  yearOfStudyId: number;
  userId: number;
}