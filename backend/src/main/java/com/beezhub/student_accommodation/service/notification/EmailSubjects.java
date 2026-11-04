package com.beezhub.student_accommodation.service.notification;

/**
 * Centralised catalogue of all email subject lines.
 * All subject strings must be referenced from this class — never defined inline at call sites.
 */
public final class EmailSubjects {

    private EmailSubjects() {}

    public static final String WELCOME = "Welcome to Student Accommodation System";
    public static final String PASSWORD_RESET = "Password Reset Request - Student Accommodation System";
    public static final String APPLICATION_APPROVED = "Application Approved - Student Accommodation";
    public static final String APPLICATION_REJECTED = "Application Status Update - Student Accommodation";
    public static final String DOCUMENT_VERIFIED = "Document Verified - Student Accommodation";
    public static final String DOCUMENT_REJECTED = "Document Review Required - Student Accommodation";
    public static final String DOCUMENT_COMMENT = "Document Review: Action Required - Student Accommodation";
    public static final String ROOM_ALLOCATED = "Action Required: Accept Your Room Offer - Student Accommodation";
    public static final String ALLOCATION_PROPOSAL = "Room Allocation Proposal - Student Accommodation";
    public static final String ALLOCATION_CONFIRMED = "Room Allocation Confirmed - Student Accommodation";
    public static final String ALLOCATION_REJECTED = "Room Allocation Update - Student Accommodation";
    public static final String ALLOCATION_MOVED = "Room Change Notification - Student Accommodation";
    public static final String OFFER_DECLINED_REQUEUE = "Room Offer Declined - You Remain in the Queue";
    public static final String OFFER_DECLINED_MAX_REACHED = "Application Cancelled - Maximum Offer Declines Reached";
    public static final String OFFER_EXPIRED = "Room Offer Expired - Student Accommodation";
    public static final String OFFER_EXPIRY_REMINDER = "Action Required: Room Offer Expiring Soon";
    public static final String CHECK_IN_REMINDER = "Reminder: Check-In Deadline Approaching";
    public static final String CHECK_IN_EXPIRED = "Check-In Deadline Expired - Application Cancelled";
}
