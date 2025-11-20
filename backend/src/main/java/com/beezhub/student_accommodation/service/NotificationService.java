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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
}
