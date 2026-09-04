export interface DocumentData {
  id: number;
  documentName: string;
  documentTypeId?: number;
  documentTypeName: string;
  fileUrl?: string;
  fileSize?: number; // File size in bytes
  documentStatus: string;
  uploadDate: string;
  // Application context
  applicationId?: number;
  applicationStatus?: string;
  // Student context
  studentId?: number;
  studentFirstName?: string;
  studentLastName?: string;
  studentEmail?: string;
  // Allocation context
  roomAllocated?: boolean;
  roomId?: number;
  roomNumber?: string;
}
