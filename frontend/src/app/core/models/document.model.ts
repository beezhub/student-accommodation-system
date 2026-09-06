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

/**
 * What the inline document viewer needs, as opposed to the full DTO.
 *
 * <p>Three screens fed the viewer through `any`, each with a different shape — and the dashboard
 * passed a raw DocumentData, whose field is `documentName`, so the viewer's `document.name` was
 * always undefined there and the downloaded file fell back to "document" (GH-158).
 */
export interface DocumentPreview {
  id: number;
  name: string;
  type?: string;
  uploadDate?: string;
  status?: string;
  size?: string;
}
