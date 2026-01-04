export interface Application {
    id: number;
    applicationCode: string;
    applicationDate: string;
    status: string;
    createdAt?: string;
    updatedAt?: string;

    // Optional student info fields
    studentId?: number;
    studentNumber?: string;
    studentName?: string;
    studentEmail?: string;

    // Optional preference fields
    preferredAccommodationType?: string;
    preferredMoveInDate?: string;
    preferredDurationMonths?: number;
    preferredRoomType?: string;
    specialRequirements?: string;
    institutionName?: string;
    courseName?: string;
}
