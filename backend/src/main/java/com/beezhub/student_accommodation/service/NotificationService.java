package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailService emailService;
    private final NotificationRepository notificationRepository;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    @Value("${app.support-email:accommodation@university.edu}")
    private String supportEmail;

    @Value("${app.university-name:University}")
    private String universityName;

    @Value("${app.notification.retry-delay-minutes:5}")
    private int retryDelayMinutes;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm");
    private static final int MAX_RETRIES = 3;

    @Async("notificationExecutor")
    public void sendApplicationStatusNotification(Application application, ApplicationStatus newStatus) {
        Student student = application.getStudent();
        AppUser user = student.getAppUser();
        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending application status notification to {} for application {}, status: {}",
                email, application.getId(), newStatus);

        NotificationType notificationType = newStatus == ApplicationStatus.APPROVED
                ? NotificationType.APPLICATION_APPROVED
                : NotificationType.APPLICATION_REJECTED;

        String subject = newStatus == ApplicationStatus.APPROVED
                ? "Application Approved - Student Accommodation"
                : "Application Status Update - Student Accommodation";

        Notification notification = createNotification(
                email, subject, notificationType, user,
                application.getId(), "Application"
        );

        try {
            switch (newStatus) {
                case APPROVED -> sendApplicationApprovedNotification(application, studentName, email);
                case REJECTED -> sendApplicationRejectedNotification(application, studentName, email);
                case PENDING -> {
                    log.debug("No notification sent for PENDING status");
                    return;
                }
                default -> {
                    log.warn("Unknown application status: {}", newStatus);
                    return;
                }
            }
            markNotificationAsSent(notification);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    private void sendApplicationApprovedNotification(Application application, String studentName, String email) {
        String applicationDate = formatDate(application.getApplicationDate());
        String portalUrl = baseUrl + "/portal/applications/" + application.getId();

        emailService.sendApplicationApprovedEmail(
                email,
                studentName,
                application.getId(),
                applicationDate,
                getCurrentAcademicYear(),
                getCurrentSemester(),
                portalUrl
        );
        log.info("Application approved notification sent to {}", email);
    }

    private void sendApplicationRejectedNotification(Application application, String studentName, String email) {
        String applicationDate = formatDate(application.getApplicationDate());
        String reviewDate = formatDate(LocalDateTime.now());
        String rejectionReason = application.getRemarks() != null
                ? application.getRemarks()
                : "Your application did not meet the current eligibility criteria.";
        String portalUrl = baseUrl + "/portal/applications/" + application.getId();

        emailService.sendApplicationRejectedEmail(
                email,
                studentName,
                application.getId(),
                rejectionReason,
                applicationDate,
                reviewDate,
                getNextApplicationPeriod(),
                portalUrl
        );
        log.info("Application rejected notification sent to {}", email);
    }

    @Async("notificationExecutor")
    public void sendDocumentVerificationNotification(Document document, DocumentStatus newStatus, String comment) {
        Student student = document.getStudent();
        AppUser user = student.getAppUser();
        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();
        String documentType = document.getDocumentType().getTypeName();

        log.info("Sending document verification notification to {} for document {}, status: {}",
                email, document.getId(), newStatus);

        NotificationType notificationType = newStatus == DocumentStatus.APPROVED
                ? NotificationType.DOCUMENT_VERIFIED
                : NotificationType.DOCUMENT_REJECTED;

        String subject = newStatus == DocumentStatus.APPROVED
                ? "Document Verified - Student Accommodation"
                : "Document Review Required - Student Accommodation";

        Notification notification = createNotification(
                email, subject, notificationType, user,
                document.getId(), "Document"
        );

        try {
            switch (newStatus) {
                case APPROVED -> sendDocumentVerifiedNotification(document, studentName, email, documentType);
                case REJECTED -> sendDocumentRejectedNotification(document, studentName, email, documentType, comment);
                case PENDING -> {
                    log.debug("No notification sent for PENDING document status");
                    return;
                }
                default -> {
                    log.warn("Unknown document status: {}", newStatus);
                    return;
                }
            }
            markNotificationAsSent(notification);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    private void sendDocumentVerifiedNotification(Document document, String studentName,
                                                  String email, String documentType) {
        String documentReference = "DOC-" + document.getId();
        String verificationDate = formatDate(document.getUpdatedAt());
        String portalUrl = baseUrl + "/portal/documents";

        emailService.sendDocumentVerifiedEmail(
                email,
                studentName,
                documentType,
                documentReference,
                verificationDate,
                "Accommodation Office",
                portalUrl
        );
        log.info("Document verified notification sent to {}", email);
    }

    private void sendDocumentRejectedNotification(Document document, String studentName,
                                                  String email, String documentType, String comment) {
        String documentReference = "DOC-" + document.getId();
        String submissionDate = formatDate(document.getCreatedAt());
        String reviewDate = formatDate(document.getUpdatedAt());
        String rejectionReason = comment != null && !comment.isEmpty()
                ? comment
                : "The document does not meet our verification requirements.";
        String resubmissionDeadline = formatDate(LocalDateTime.now().plusDays(7));
        String uploadUrl = baseUrl + "/portal/documents/upload";

        Map<String, Object> additionalVariables = new HashMap<>();
        if (comment != null && !comment.isEmpty()) {
            List<String> feedbackPoints = parseFeedbackPoints(comment);
            if (!feedbackPoints.isEmpty()) {
                additionalVariables.put("feedbackPoints", feedbackPoints);
            }
        }

        emailService.sendDocumentRejectedEmail(
                email,
                studentName,
                documentType,
                documentReference,
                submissionDate,
                reviewDate,
                rejectionReason,
                resubmissionDeadline,
                uploadUrl,
                additionalVariables
        );
        log.info("Document rejected notification sent to {}", email);
    }

    /**
     * Sends a notification to a student when an admin adds a comment to their document.
     *
     * @param document    The document that received the comment
     * @param commentText The comment text from the admin
     */
    @Async("notificationExecutor")
    public void sendDocumentCommentNotification(Document document, String commentText) {
        Student student = document.getStudent();
        AppUser user = student.getAppUser();
        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();
        String documentName = document.getDocumentName();
        String documentType = document.getDocumentType().getTypeName();

        log.info("Sending document comment notification to {} for document {}", email, document.getId());

        Notification notification = createNotification(
                email,
                "Document Requires Attention - Student Accommodation",
                NotificationType.DOCUMENT_REJECTED,
                user,
                document.getId(),
                "Document"
        );

        try {
            String documentReference = "DOC-" + document.getId();
            String portalUrl = baseUrl + "/portal/documents";

            String htmlContent = buildDocumentCommentEmailContent(
                    studentName,
                    documentName,
                    documentType,
                    documentReference,
                    commentText,
                    portalUrl
            );

            emailService.sendHtmlEmail(email, notification.getSubject(), htmlContent);

            markNotificationAsSent(notification);
            log.info("Document comment notification sent to {}", email);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    private String buildDocumentCommentEmailContent(String studentName, String documentName,
                                                     String documentType, String documentReference,
                                                     String commentText, String portalUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 20px auto; background: #fff;
                                    border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%);
                                 color: #fff; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { padding: 30px; }
                        .info-box { background: #f8f9fa; border-left: 4px solid #667eea; padding: 15px; margin: 20px 0; }
                        .comment-box { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                        .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                 color: #fff; padding: 12px 30px; text-decoration: none;
                                 border-radius: 5px; margin: 20px 0; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center;
                                 font-size: 14px; color: #666; border-radius: 0 0 8px 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Document Requires Attention</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <p>An administrator has added a comment to your document that requires your attention.</p>
                            <div class="info-box">
                                <strong>Document Details:</strong>
                                <ul>
                                    <li><strong>Document:</strong> %s</li>
                                    <li><strong>Type:</strong> %s</li>
                                    <li><strong>Reference:</strong> %s</li>
                                </ul>
                            </div>
                            <div class="comment-box">
                                <strong>Admin Comment:</strong>
                                <p>%s</p>
                            </div>
                            <p>Please review the comment and take appropriate action. You may need to resubmit the document with the requested changes.</p>
                            <center>
                                <a href="%s" class="button">View Documents</a>
                            </center>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from the Student Accommodation System.</p>
                            <p>If you have any questions, please contact our support team at %s</p>
                            <p>&copy; 2025 %s Student Accommodation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(studentName, documentName, documentType, documentReference,
                              commentText, portalUrl, supportEmail, universityName);
    }

    @Async("notificationExecutor")
    public void sendRoomAllocationNotification(RoomAllocation allocation) {
        Student student = allocation.getStudent();
        AppUser user = student.getAppUser();
        Room room = allocation.getRoom();
        Building building = room.getBuilding();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending room allocation notification to {} for allocation {}", email, allocation.getId());

        Notification notification = createNotification(
                email,
                "Room Allocation Confirmation - Student Accommodation",
                NotificationType.ROOM_ALLOCATED,
                user,
                allocation.getId(),
                "RoomAllocation"
        );

        try {
            String roomNumber = room.getRoomNumber();
            String buildingName = building.getName();
            String buildingAddress = building.getAddress() != null ? building.getAddress() : "";
            Integer floorNumber = extractFloorNumber(roomNumber);
            String roomType = determineRoomType(room);
            String bedNumber = "1";
            String allocationId = "ALLOC-" + allocation.getId();
            String startDate = formatDate(allocation.getAllocationDate());
            String endDate = allocation.getEndDate() != null
                    ? formatDate(allocation.getEndDate())
                    : formatDate(allocation.getAllocationDate().plusMonths(10));
            String duration = calculateDuration(allocation.getAllocationDate(), allocation.getEndDate());
            String officeLocation = "Main Administration Building, Ground Floor";
            String portalUrl = baseUrl + "/portal/allocations/" + allocation.getId();

            emailService.sendRoomAllocatedEmail(
                    email,
                    studentName,
                    roomNumber,
                    buildingName,
                    floorNumber,
                    roomType,
                    bedNumber,
                    allocationId,
                    startDate,
                    endDate,
                    duration,
                    officeLocation,
                    buildingAddress,
                    portalUrl
            );

            markNotificationAsSent(notification);
            log.info("Room allocation notification sent to {}", email);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    /**
     * Sends a notification when a staged allocation (proposal) is created for a student.
     *
     * @param stagedAllocation The staged allocation proposal
     */
    @Async("notificationExecutor")
    public void sendAllocationProposalNotification(StagedAllocation stagedAllocation) {
        Student student = stagedAllocation.getStudent();
        AppUser user = student.getAppUser();
        Room room = stagedAllocation.getRoom();
        Building building = room.getBuilding();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending allocation proposal notification to {} for staged allocation {}", email, stagedAllocation.getId());

        Notification notification = createNotification(
                email,
                "Room Allocation Proposal - Student Accommodation",
                NotificationType.ROOM_ALLOCATED,
                user,
                stagedAllocation.getId(),
                "StagedAllocation"
        );

        try {
            String roomNumber = room.getRoomNumber();
            String buildingName = building.getName();
            Integer floorNumber = extractFloorNumber(roomNumber);
            Integer roomCapacity = room.getCapacity();
            String proposalId = "PROP-" + stagedAllocation.getId();
            BigDecimal score = stagedAllocation.getScore();
            List<String> warnings = parseWarnings(stagedAllocation.getWarnings());
            // Move-in date removed as it's no longer collected in applications
            String moveInDate = null;
            String portalUrl = baseUrl + "/portal/allocations/proposal/" + stagedAllocation.getId();

            emailService.sendAllocationProposalEmail(
                    email,
                    studentName,
                    roomNumber,
                    buildingName,
                    floorNumber,
                    roomCapacity,
                    proposalId,
                    score,
                    warnings,
                    moveInDate,
                    portalUrl,
                    supportEmail
            );

            markNotificationAsSent(notification);
            log.info("Allocation proposal notification sent to {}", email);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    /**
     * Sends a notification when a staged allocation is confirmed and converted to a room allocation.
     *
     * @param allocation The confirmed room allocation
     */
    @Async("notificationExecutor")
    public void sendAllocationConfirmedNotification(RoomAllocation allocation) {
        Student student = allocation.getStudent();
        AppUser user = student.getAppUser();
        Room room = allocation.getRoom();
        Building building = room.getBuilding();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending allocation confirmed notification to {} for allocation {}", email, allocation.getId());

        Notification notification = createNotification(
                email,
                "Room Allocation Confirmed - Student Accommodation",
                NotificationType.ROOM_ALLOCATED,
                user,
                allocation.getId(),
                "RoomAllocation"
        );

        try {
            String roomNumber = room.getRoomNumber();
            String buildingName = building.getName();
            Integer floorNumber = extractFloorNumber(roomNumber);
            String roomType = determineRoomType(room);
            String allocationId = "ALLOC-" + allocation.getId();
            String moveInDate = formatDate(allocation.getAllocationDate());
            String startDate = formatDate(allocation.getAllocationDate());
            String endDate = allocation.getEndDate() != null
                    ? formatDate(allocation.getEndDate())
                    : formatDate(allocation.getAllocationDate().plusMonths(10));
            String portalUrl = baseUrl + "/portal/allocations/" + allocation.getId();

            emailService.sendAllocationConfirmedEmail(
                    email,
                    studentName,
                    roomNumber,
                    buildingName,
                    floorNumber,
                    roomType,
                    allocationId,
                    moveInDate,
                    startDate,
                    endDate,
                    portalUrl,
                    supportEmail,
                    null
            );

            markNotificationAsSent(notification);
            log.info("Allocation confirmed notification sent to {}", email);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    /**
     * Sends a notification when a staged allocation proposal is rejected.
     *
     * @param stagedAllocation The rejected staged allocation
     * @param reason           Optional reason for rejection
     * @param waitingListPosition Optional waiting list position
     */
    @Async("notificationExecutor")
    public void sendAllocationRejectedNotification(StagedAllocation stagedAllocation, String reason, Integer waitingListPosition) {
        Student student = stagedAllocation.getStudent();
        AppUser user = student.getAppUser();
        Room room = stagedAllocation.getRoom();
        Building building = room.getBuilding();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending allocation rejected notification to {} for staged allocation {}", email, stagedAllocation.getId());

        Notification notification = createNotification(
                email,
                "Room Allocation Update - Student Accommodation",
                NotificationType.APPLICATION_REJECTED,
                user,
                stagedAllocation.getId(),
                "StagedAllocation"
        );

        try {
            String roomNumber = room.getRoomNumber();
            String buildingName = building.getName();
            String proposalId = "PROP-" + stagedAllocation.getId();
            String portalUrl = baseUrl + "/portal/applications";

            emailService.sendAllocationRejectedEmail(
                    email,
                    studentName,
                    roomNumber,
                    buildingName,
                    proposalId,
                    reason,
                    waitingListPosition,
                    portalUrl,
                    supportEmail,
                    null
            );

            markNotificationAsSent(notification);
            log.info("Allocation rejected notification sent to {}", email);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    /**
     * Sends a notification when a student's room allocation is moved to a different room.
     *
     * @param newAllocation The new room allocation
     * @param oldRoom       The previous room
     * @param effectiveDate The date the move is effective
     * @param reason        Optional reason for the move
     */
    @Async("notificationExecutor")
    public void sendAllocationMovedNotification(RoomAllocation newAllocation, Room oldRoom, LocalDateTime effectiveDate, String reason) {
        Student student = newAllocation.getStudent();
        AppUser user = student.getAppUser();
        Room newRoom = newAllocation.getRoom();
        Building newBuilding = newRoom.getBuilding();
        Building oldBuilding = oldRoom.getBuilding();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending allocation moved notification to {} for allocation {}", email, newAllocation.getId());

        Notification notification = createNotification(
                email,
                "Room Change Notification - Student Accommodation",
                NotificationType.ROOM_ALLOCATED,
                user,
                newAllocation.getId(),
                "RoomAllocation"
        );

        try {
            String oldRoomNumber = oldRoom.getRoomNumber();
            String oldBuildingName = oldBuilding.getName();
            Integer oldFloorNumber = extractFloorNumber(oldRoomNumber);
            String newRoomNumber = newRoom.getRoomNumber();
            String newBuildingName = newBuilding.getName();
            Integer newFloorNumber = extractFloorNumber(newRoomNumber);
            String newRoomType = determineRoomType(newRoom);
            String effectiveDateStr = formatDate(effectiveDate);
            String portalUrl = baseUrl + "/portal/allocations/" + newAllocation.getId();

            emailService.sendAllocationMovedEmail(
                    email,
                    studentName,
                    oldRoomNumber,
                    oldBuildingName,
                    oldFloorNumber,
                    newRoomNumber,
                    newBuildingName,
                    newFloorNumber,
                    newRoomType,
                    effectiveDateStr,
                    reason,
                    portalUrl,
                    supportEmail,
                    null
            );

            markNotificationAsSent(notification);
            log.info("Allocation moved notification sent to {}", email);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    /**
     * Sends a notification to a student when an admin adds a comment to their document
     * using the new document-comment template.
     *
     * @param document     The document that received the comment
     * @param commentText  The comment text from the admin
     * @param reviewerName The name of the reviewer who added the comment
     */
    @Async("notificationExecutor")
    public void sendDocumentCommentNotificationWithTemplate(Document document, String commentText, String reviewerName) {
        Student student = document.getStudent();
        AppUser user = student.getAppUser();
        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();
        String documentName = document.getDocumentName();
        String documentType = document.getDocumentType().getTypeName();

        log.info("Sending document comment notification to {} for document {}", email, document.getId());

        Notification notification = createNotification(
                email,
                "Document Review: Action Required - Student Accommodation",
                NotificationType.DOCUMENT_REJECTED,
                user,
                document.getId(),
                "Document"
        );

        try {
            String applicationId = document.getApplication() != null
                    ? "APP-" + document.getApplication().getId()
                    : "N/A";
            String uploadDate = formatDate(document.getCreatedAt());
            String documentStatus = document.getDocumentStatus() != null ? document.getDocumentStatus().name() : "PENDING_REVIEW";
            String commentDate = formatDate(LocalDateTime.now());
            String portalUrl = baseUrl + "/portal/documents";

            emailService.sendDocumentCommentEmail(
                    email,
                    studentName,
                    documentType,
                    documentName,
                    applicationId,
                    uploadDate,
                    documentStatus,
                    commentText,
                    reviewerName,
                    commentDate,
                    portalUrl,
                    supportEmail,
                    null
            );

            markNotificationAsSent(notification);
            log.info("Document comment notification sent to {}", email);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    /**
     * Sends a notification when a student declines an offer and is re-queued.
     *
     * @param allocation The declined room allocation
     * @param totalDeclineCount The total number of declines including this one
     * @param maxDeclineCount The maximum allowed declines
     */
    @Async("notificationExecutor")
    public void sendOfferDeclinedRequeueNotification(RoomAllocation allocation, int totalDeclineCount, int maxDeclineCount) {
        Student student = allocation.getStudent();
        AppUser user = student.getAppUser();
        Room room = allocation.getRoom();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending offer declined re-queue notification to {} for allocation {}", email, allocation.getId());

        Notification notification = createNotification(
                email,
                "Room Offer Declined - Student Accommodation",
                NotificationType.ROOM_ALLOCATED,
                user,
                allocation.getId(),
                "RoomAllocation"
        );

        try {
            String roomNumber = room.getRoomNumber();
            String buildingName = room.getBuilding() != null ? room.getBuilding().getName() : "";
            int declinesRemaining = maxDeclineCount - totalDeclineCount;
            String declineReason = allocation.getDeclineReason() != null
                    ? allocation.getDeclineReason().getDisplayName()
                    : "Not specified";
            String portalUrl = baseUrl + "/portal/applications";

            String htmlContent = buildOfferDeclinedRequeueEmailContent(
                    studentName,
                    roomNumber,
                    buildingName,
                    totalDeclineCount,
                    declinesRemaining,
                    declineReason,
                    portalUrl
            );

            emailService.sendHtmlEmail(email, notification.getSubject(), htmlContent);

            markNotificationAsSent(notification);
            log.info("Offer declined re-queue notification sent to {}", email);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    /**
     * Sends a notification when a student reaches the maximum decline limit.
     *
     * @param allocation The final declined room allocation
     * @param totalDeclineCount The total number of declines
     */
    @Async("notificationExecutor")
    public void sendOfferDeclinedMaxReachedNotification(RoomAllocation allocation, int totalDeclineCount) {
        Student student = allocation.getStudent();
        AppUser user = student.getAppUser();
        Application application = allocation.getApplication();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending offer declined max reached notification to {} for allocation {}", email, allocation.getId());

        Notification notification = createNotification(
                email,
                "Application Closed - Student Accommodation",
                NotificationType.APPLICATION_REJECTED,
                user,
                allocation.getId(),
                "RoomAllocation"
        );

        try {
            String applicationCode = application != null ? application.getApplicationCode() : "N/A";
            String portalUrl = baseUrl + "/portal/applications";

            String htmlContent = buildOfferDeclinedMaxReachedEmailContent(
                    studentName,
                    totalDeclineCount,
                    applicationCode,
                    portalUrl
            );

            emailService.sendHtmlEmail(email, notification.getSubject(), htmlContent);

            markNotificationAsSent(notification);
            log.info("Offer declined max reached notification sent to {}", email);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    @Async("notificationExecutor")
    public void sendWelcomeEmail(AppUser user) {
        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending welcome email to {}", email);

        Notification notification = createNotification(
                email,
                "Welcome to Student Accommodation System",
                NotificationType.WELCOME_EMAIL,
                user,
                user.getId(),
                "AppUser"
        );

        try {
            String studentId = "N/A";
            String registrationDate = formatDate(user.getCreatedAt());
            String loginUrl = baseUrl + "/login";
            String guideUrl = baseUrl + "/guide";

            emailService.sendWelcomeEmail(
                    email,
                    studentName,
                    email,
                    studentId,
                    registrationDate,
                    loginUrl,
                    guideUrl
            );

            markNotificationAsSent(notification);
            log.info("Welcome email sent to {}", email);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    @Async("notificationExecutor")
    public void sendPasswordResetEmail(AppUser user, String resetToken) {
        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending password reset email to {}", email);

        Notification notification = createNotification(
                email,
                "Password Reset Request - Student Accommodation System",
                NotificationType.PASSWORD_RESET,
                user,
                user.getId(),
                "AppUser"
        );

        try {
            String resetUrl = baseUrl + "/reset-password?token=" + resetToken;
            String htmlContent = buildPasswordResetEmailContent(studentName, resetUrl);

            emailService.sendHtmlEmail(email, notification.getSubject(), htmlContent);

            markNotificationAsSent(notification);
            log.info("Password reset email sent to {}", email);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    @Scheduled(fixedDelayString = "${app.notification.retry-check-interval-ms:300000}")
    @Transactional
    public void retryFailedNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(retryDelayMinutes);
        List<Notification> failedNotifications = notificationRepository
                .findFailedNotificationsForRetry(NotificationStatus.FAILED, threshold);

        if (!failedNotifications.isEmpty()) {
            log.info("Found {} failed notifications to retry", failedNotifications.size());

            for (Notification notification : failedNotifications) {
                if (notification.canRetry()) {
                    retryNotification(notification);
                } else {
                    log.warn("Notification {} exceeded max retries ({}), marking as permanently failed",
                            notification.getId(), MAX_RETRIES);
                }
            }
        }
    }

    @Async("notificationExecutor")
    @Transactional
    public void retryNotification(Notification notification) {
        log.info("Retrying notification {} (attempt {}/{})",
                notification.getId(), notification.getRetryCount() + 1, MAX_RETRIES);

        notification.markAsRetrying();
        notification.incrementRetryCount();
        notificationRepository.save(notification);

        try {
            Thread.sleep(getExponentialBackoff(notification.getRetryCount()));

            switch (notification.getType()) {
                case PASSWORD_RESET -> log.warn("Password reset tokens may have expired, skipping retry");
                default -> log.info("Retry logic for {} not yet implemented", notification.getType());
            }

        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    private long getExponentialBackoff(int retryCount) {
        return (long) Math.pow(2, retryCount) * 1000;
    }

    @Transactional
    protected Notification createNotification(String recipient, String subject,
                                            NotificationType type, AppUser user,
                                            Long referenceId, String referenceType) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .subject(subject)
                .type(type)
                .status(NotificationStatus.PENDING)
                .user(user)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .retryCount(0)
                .maxRetries(MAX_RETRIES)
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    protected void markNotificationAsSent(Notification notification) {
        notification.markAsSent();
        notificationRepository.save(notification);
        log.debug("Notification {} marked as sent", notification.getId());
    }

    @Transactional
    protected void handleNotificationFailure(Notification notification, Exception e) {
        String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        notification.markAsFailed(errorMessage);
        notificationRepository.save(notification);

        log.error("Failed to send notification {} (type: {}) to {}. Error: {}. Will retry if under max attempts.",
                notification.getId(), notification.getType(), notification.getRecipient(), errorMessage, e);
    }

    private String buildPasswordResetEmailContent(String studentName, String resetUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 20px auto; background: #fff;
                                    border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                 color: #fff; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { padding: 30px; }
                        .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                 color: #fff; padding: 12px 30px; text-decoration: none;
                                 border-radius: 5px; margin: 20px 0; }
                        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center;
                                 font-size: 14px; color: #666; border-radius: 0 0 8px 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Password Reset Request</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <p>We received a request to reset your password for your Student Accommodation System account.</p>
                            <p>Click the button below to reset your password:</p>
                            <center>
                                <a href="%s" class="button">Reset Password</a>
                            </center>
                            <div class="warning">
                                <strong>⚠️ Security Notice:</strong>
                                <ul>
                                    <li>This link will expire in 1 hour</li>
                                    <li>If you didn't request this reset, please ignore this email</li>
                                    <li>Never share this link with anyone</li>
                                </ul>
                            </div>
                            <p>If the button doesn't work, copy and paste this link into your browser:</p>
                            <p style="word-break: break-all; color: #667eea;">%s</p>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from the Student Accommodation System.</p>
                            <p>&copy; 2025 Student Accommodation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(studentName, resetUrl, resetUrl);
    }

    private String buildOfferDeclinedRequeueEmailContent(String studentName, String roomNumber,
                                                          String buildingName, int totalDeclineCount,
                                                          int declinesRemaining, String declineReason,
                                                          String portalUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 20px auto; background: #fff;
                                    border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #ffa500 0%%, #ff6b6b 100%%);
                                 color: #fff; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { padding: 30px; }
                        .info-box { background: #f8f9fa; border-left: 4px solid #ffa500; padding: 15px; margin: 20px 0; }
                        .warning-box { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                        .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                 color: #fff; padding: 12px 30px; text-decoration: none;
                                 border-radius: 5px; margin: 20px 0; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center;
                                 font-size: 14px; color: #666; border-radius: 0 0 8px 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Room Offer Declined</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <p>We have received your decision to decline the room offer. Your application has been updated accordingly.</p>
                            <div class="info-box">
                                <strong>Declined Room Details:</strong>
                                <ul>
                                    <li><strong>Room:</strong> %s</li>
                                    <li><strong>Building:</strong> %s</li>
                                    <li><strong>Reason:</strong> %s</li>
                                </ul>
                            </div>
                            <div class="warning-box">
                                <strong>⚠️ Decline Status:</strong>
                                <p>You have declined <strong>%d of 2</strong> allowed offers.</p>
                                <p>You have <strong>%d decline(s) remaining</strong>.</p>
                                <p>Your application status has been updated to <strong>AWAITING ALLOCATION</strong> and you have been added back to the waiting list. We will notify you when another room becomes available.</p>
                            </div>
                            <center>
                                <a href="%s" class="button">View Application Status</a>
                            </center>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from the Student Accommodation System.</p>
                            <p>If you have any questions, please contact our support team at %s</p>
                            <p>&copy; 2025 %s Student Accommodation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(studentName, roomNumber, buildingName, declineReason,
                              totalDeclineCount, declinesRemaining, portalUrl, supportEmail, universityName);
    }

    private String buildOfferDeclinedMaxReachedEmailContent(String studentName, int totalDeclineCount,
                                                             String applicationCode, String portalUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 20px auto; background: #fff;
                                    border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #dc3545 0%%, #c82333 100%%);
                                 color: #fff; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { padding: 30px; }
                        .alert-box { background: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; }
                        .info-box { background: #f8f9fa; border-left: 4px solid #6c757d; padding: 15px; margin: 20px 0; }
                        .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                 color: #fff; padding: 12px 30px; text-decoration: none;
                                 border-radius: 5px; margin: 20px 0; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center;
                                 font-size: 14px; color: #666; border-radius: 0 0 8px 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Application Closed</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <div class="alert-box">
                                <strong>⛔ Maximum Decline Limit Reached</strong>
                                <p>You have declined <strong>%d room offers</strong>, which is the maximum allowed.</p>
                                <p>As a result, your application (<strong>%s</strong>) has been <strong>CANCELLED</strong> and you have been removed from the waiting list.</p>
                            </div>
                            <div class="info-box">
                                <strong>What This Means:</strong>
                                <ul>
                                    <li>Your current application is now closed</li>
                                    <li>You will not receive any further room offers for this application</li>
                                    <li>You may submit a new application in the next application period</li>
                                </ul>
                            </div>
                            <p>If you have any questions or would like to discuss alternative accommodation options, please contact our accommodation office.</p>
                            <center>
                                <a href="%s" class="button">View Application</a>
                            </center>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from the Student Accommodation System.</p>
                            <p>For assistance, please contact our support team at %s</p>
                            <p>&copy; 2025 %s Student Accommodation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(studentName, totalDeclineCount, applicationCode, portalUrl, supportEmail, universityName);
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DATE_FORMATTER);
    }

    private String formatDate(java.time.LocalDate date) {
        if (date == null) return "N/A";
        return date.format(DATE_FORMATTER);
    }

    private String getCurrentAcademicYear() {
        int currentYear = LocalDateTime.now().getYear();
        return currentYear + "/" + (currentYear + 1);
    }

    private String getCurrentSemester() {
        int currentMonth = LocalDateTime.now().getMonthValue();
        return currentMonth >= 1 && currentMonth <= 6 ? "First Semester" : "Second Semester";
    }

    private String getNextApplicationPeriod() {
        int currentMonth = LocalDateTime.now().getMonthValue();
        int currentYear = LocalDateTime.now().getYear();

        if (currentMonth >= 1 && currentMonth <= 6) {
            return "June " + currentYear;
        } else {
            return "January " + (currentYear + 1);
        }
    }

    private List<String> parseFeedbackPoints(String comment) {
        if (comment == null || comment.isEmpty()) {
            return List.of();
        }

        if (comment.contains(";")) {
            return List.of(comment.split(";"));
        } else if (comment.contains("\n")) {
            return List.of(comment.split("\n"));
        }

        return List.of();
    }

    private Integer extractFloorNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.length() < 2) return 1;

        try {
            String floorPart = roomNumber.substring(0, 1);
            return Integer.parseInt(floorPart);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private String determineRoomType(Room room) {
        if (room.getCapacity() == null) return "Standard";

        return switch (room.getCapacity()) {
            case 1 -> "Single";
            case 2 -> "Double";
            case 3 -> "Triple";
            case 4 -> "Quad";
            default -> "Shared (" + room.getCapacity() + " beds)";
        };
    }

    private String calculateDuration(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) return "N/A";
        if (endDate == null) {
            endDate = startDate.plusMonths(10);
        }

        long months = ChronoUnit.MONTHS.between(startDate, endDate);

        if (months == 1) return "1 month";
        if (months < 12) return months + " months";

        long years = months / 12;
        long remainingMonths = months % 12;

        if (remainingMonths == 0) {
            return years == 1 ? "1 year" : years + " years";
        }

        return years + " year" + (years > 1 ? "s" : "") + " and " +
               remainingMonths + " month" + (remainingMonths > 1 ? "s" : "");
    }

    private List<String> parseWarnings(String warningsJson) {
        if (warningsJson == null || warningsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(warningsJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse warnings JSON: {}", warningsJson, e);
            return new ArrayList<>();
        }
    }

    @Async("notificationExecutor")
    public void sendOfferExpiredNotification(RoomAllocation allocation) {
        Student student = allocation.getStudent();
        AppUser user = student.getAppUser();
        Application application = allocation.getApplication();
        Room room = allocation.getRoom();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending offer expired notification to {} for allocation {}", email, allocation.getId());

        Notification notification = createNotification(
                email,
                "Room Offer Expired - Student Accommodation",
                NotificationType.APPLICATION_REJECTED,
                user,
                allocation.getId(),
                "RoomAllocation"
        );

        try {
            String applicationCode = application != null ? application.getApplicationCode() : "N/A";
            String roomNumber = room.getRoomNumber();
            String buildingName = room.getBuilding() != null ? room.getBuilding().getName() : "Unknown Building";
            String portalUrl = baseUrl + "/portal/applications";

            String htmlContent = buildOfferExpiredEmailContent(
                    studentName,
                    applicationCode,
                    roomNumber,
                    buildingName,
                    portalUrl
            );

            emailService.sendHtmlEmail(email, notification.getSubject(), htmlContent);

            markNotificationAsSent(notification);
            log.info("Offer expired notification sent to {}", email);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    @Async("notificationExecutor")
    public void sendCheckInExpiredNotification(RoomAllocation allocation) {
        Student student = allocation.getStudent();
        AppUser user = student.getAppUser();
        Application application = allocation.getApplication();
        Room room = allocation.getRoom();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending check-in expired notification to {} for allocation {}", email, allocation.getId());

        Notification notification = createNotification(
                email,
                "Check-In Deadline Expired - Student Accommodation",
                NotificationType.APPLICATION_REJECTED,
                user,
                allocation.getId(),
                "RoomAllocation"
        );

        try {
            String applicationCode = application != null ? application.getApplicationCode() : "N/A";
            String roomNumber = room.getRoomNumber();
            String buildingName = room.getBuilding() != null ? room.getBuilding().getName() : "Unknown Building";
            String portalUrl = baseUrl + "/portal/applications";

            String htmlContent = buildCheckInExpiredEmailContent(
                    studentName,
                    applicationCode,
                    roomNumber,
                    buildingName,
                    portalUrl
            );

            emailService.sendHtmlEmail(email, notification.getSubject(), htmlContent);

            markNotificationAsSent(notification);
            log.info("Check-in expired notification sent to {}", email);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    private String buildOfferExpiredEmailContent(String studentName, String applicationCode,
                                                  String roomNumber, String buildingName, String portalUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 20px auto; background: #fff;
                                    border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%);
                                 color: #fff; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { padding: 30px; }
                        .alert-box { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; }
                        .info-box { background: #f8f9fa; border-left: 4px solid #6c757d; padding: 15px; margin: 20px 0; }
                        .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                 color: #fff; padding: 12px 30px; text-decoration: none;
                                 border-radius: 5px; margin: 20px 0; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center;
                                 font-size: 14px; color: #666; border-radius: 0 0 8px 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>⏰ Room Offer Expired</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <div class="alert-box">
                                <strong>Your room offer has expired</strong>
                                <p>The offer for <strong>Room %s</strong> in <strong>%s</strong> has expired because no response was received within 7 days.</p>
                                <p>As a result, your application (<strong>%s</strong>) has been <strong>CANCELLED</strong>.</p>
                            </div>
                            <div class="info-box">
                                <strong>What This Means:</strong>
                                <ul>
                                    <li>Your current application is now closed</li>
                                    <li>The room has been released for other students</li>
                                    <li>You will need to submit a new application if you still need accommodation</li>
                                </ul>
                            </div>
                            <p>If you still require accommodation, please submit a new application during the next application period.</p>
                            <p>If you have any questions, please contact our accommodation office.</p>
                            <center>
                                <a href="%s" class="button">View Application</a>
                            </center>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from the Student Accommodation System.</p>
                            <p>For assistance, please contact our support team at %s</p>
                            <p>&copy; 2025 %s Student Accommodation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(studentName, roomNumber, buildingName, applicationCode, portalUrl, supportEmail, universityName);
    }

    private String buildCheckInExpiredEmailContent(String studentName, String applicationCode,
                                                    String roomNumber, String buildingName, String portalUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 20px auto; background: #fff;
                                    border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #dc3545 0%%, #c82333 100%%);
                                 color: #fff; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { padding: 30px; }
                        .alert-box { background: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; }
                        .info-box { background: #f8f9fa; border-left: 4px solid #6c757d; padding: 15px; margin: 20px 0; }
                        .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                 color: #fff; padding: 12px 30px; text-decoration: none;
                                 border-radius: 5px; margin: 20px 0; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center;
                                 font-size: 14px; color: #666; border-radius: 0 0 8px 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🚫 Check-In Deadline Expired</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <div class="alert-box">
                                <strong>Check-In Deadline Missed</strong>
                                <p>You failed to check in to <strong>Room %s</strong> in <strong>%s</strong> within 7 days of accepting the offer.</p>
                                <p>As a result, your application (<strong>%s</strong>) has been <strong>CANCELLED</strong>.</p>
                            </div>
                            <div class="info-box">
                                <strong>What This Means:</strong>
                                <ul>
                                    <li>Your room allocation has been cancelled</li>
                                    <li>The room has been released for other students</li>
                                    <li>Your current application is now closed</li>
                                    <li>You will need to submit a new application if you still need accommodation</li>
                                </ul>
                            </div>
                            <p>If you still require accommodation, please submit a new application during the next application period.</p>
                            <p>If you have any questions or concerns, please contact our accommodation office.</p>
                            <center>
                                <a href="%s" class="button">View Application</a>
                            </center>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from the Student Accommodation System.</p>
                            <p>For assistance, please contact our support team at %s</p>
                            <p>&copy; 2025 %s Student Accommodation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(studentName, roomNumber, buildingName, applicationCode, portalUrl, supportEmail, universityName);
    }
}
