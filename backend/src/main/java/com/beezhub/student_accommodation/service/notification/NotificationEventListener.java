package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.*;
import com.beezhub.student_accommodation.model.event.*;
import com.beezhub.student_accommodation.repository.NotificationRepository;
import com.beezhub.student_accommodation.service.NotificationService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final EmailDispatchService emailDispatchService;

    /** Creates a templateData map with {@code templateName} pre-populated. */
    private static Map<String, Object> td(String templateName, Map<String, Object> vars) {
        Map<String, Object> data = new HashMap<>(vars);
        data.put("templateName", templateName);
        return data;
    }

    /** Null-safe string coercion: returns the value if non-null, otherwise empty string. */
    private static Object ns(Object value) {
        return value != null ? value : "";
    }

    /** Builds a Map from key-value pairs, coercing null values to empty string. */
    private static Map<String, Object> mapOf(Object... keysAndValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keysAndValues.length - 1; i += 2) {
            map.put((String) keysAndValues[i], ns(keysAndValues[i + 1]));
        }
        return map;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        Application application = event.application();
        TenantContext.setTenantId(application.getTenantId());
        try {
            ApplicationStatus newStatus = event.newStatus();

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
                    ? EmailSubjects.APPLICATION_APPROVED
                    : EmailSubjects.APPLICATION_REJECTED;

            Notification notification = notificationService.createNotification(
                    email, subject, notificationType, user,
                    application.getId(), "Application",
                    td(newStatus == ApplicationStatus.APPROVED ? "application-approved" : "application-rejected",
                            mapOf("studentName", studentName, "applicationId", application.getId(),
                                    "email", email)));

            try {
                switch (newStatus) {
                    case APPROVED -> emailDispatchService.sendApplicationApprovedEmail(application, studentName, email);
                    case REJECTED -> emailDispatchService.sendApplicationRejectedEmail(application, studentName, email);
                    case CHANGES_REQUESTED -> log.debug("Notification for status {} not implemented yet", newStatus);
                    case PENDING -> {
                        log.debug("No notification sent for PENDING status");
                        return;
                    }
                    default -> {
                        log.warn("Unknown application status: {}", newStatus);
                        return;
                    }
                }
                notificationService.markNotificationAsSent(notification);
            } catch (Exception e) {
                notificationService.handleNotificationFailure(notification, e);
            }
        } finally {
            TenantContext.clear();
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentVerification(DocumentVerificationEvent event) {
        Document document = event.document();
        TenantContext.setTenantId(document.getTenantId());
        try {
            DocumentStatus newStatus = event.newStatus();
            String comment = event.comment();

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
                    ? EmailSubjects.DOCUMENT_VERIFIED
                    : EmailSubjects.DOCUMENT_REJECTED;

            Notification notification = notificationService.createNotification(
                    email, subject, notificationType, user,
                    document.getId(), "Document",
                    td(newStatus == DocumentStatus.APPROVED ? "document-verified" : "document-rejected",
                            mapOf("studentName", studentName, "documentType", documentType,
                                    "documentId", document.getId(), "email", email)));

            try {
                switch (newStatus) {
                    case APPROVED ->
                        emailDispatchService.sendDocumentVerifiedEmail(document, studentName, email, documentType);
                    case REJECTED ->
                        emailDispatchService.sendDocumentRejectedEmail(document, studentName, email, documentType, comment);
                    case PENDING -> {
                        log.debug("No notification sent for PENDING document status");
                        return;
                    }
                    default -> {
                        log.warn("Unknown document status: {}", newStatus);
                        return;
                    }
                }
                notificationService.markNotificationAsSent(notification);
            } catch (Exception e) {
                notificationService.handleNotificationFailure(notification, e);
            }
        } finally {
            TenantContext.clear();
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentComment(DocumentCommentEvent event) {
        Document document = event.document();
        TenantContext.setTenantId(document.getTenantId());
        try {
            String commentText = event.comment();

            Student student = document.getStudent();
            AppUser user = student.getAppUser();
            String studentName = user.getFirstName() + " " + user.getLastName();
            String email = user.getEmail();
            String documentName = document.getDocumentName();
            String documentType = document.getDocumentType().getTypeName();

            log.info("Sending document comment notification to {} for document {}", email, document.getId());

            Notification notification = notificationService.createNotification(
                    email,
                    EmailSubjects.DOCUMENT_COMMENT,
                    NotificationType.DOCUMENT_REJECTED,
                    user,
                    document.getId(),
                    "Document",
                    td("document-comment", mapOf("studentName", studentName, "documentName", documentName,
                            "documentType", documentType, "commentText", commentText,
                            "email", email)));

            try {
                emailDispatchService.sendDocumentCommentEmailWithTemplate(
                        document, commentText, "Accommodation Office", studentName, email,
                        documentName, documentType);

                notificationService.markNotificationAsSent(notification);
            } catch (Exception e) {
                notificationService.handleNotificationFailure(notification, e);
            }
        } finally {
            TenantContext.clear();
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAllocationCreated(AllocationCreatedEvent event) {
        RoomAllocation allocation = event.allocation();
        TenantContext.setTenantId(allocation.getTenantId());
        try {
            Student student = allocation.getStudent();
            AppUser user = student.getAppUser();
            Room room = allocation.getRoom();
            Building building = room.getBuilding();

            String studentName = user.getFirstName() + " " + user.getLastName();
            String email = user.getEmail();

            log.info("Sending room allocation notification to {} for allocation {}", email, allocation.getId());

            Notification notification = notificationService.createNotification(
                    email,
                    EmailSubjects.ROOM_ALLOCATED,
                    NotificationType.ROOM_ALLOCATED,
                    user,
                    allocation.getId(),
                    "RoomAllocation",
                    td("room-allocated", mapOf("studentName", studentName, "roomNumber", room.getRoomNumber(),
                            "buildingName", room.getBuilding().getName(), "allocationId", allocation.getId(),
                            "email", email)));

            try {
                emailDispatchService.sendRoomAllocatedEmail(allocation, room, building, studentName, email);
                notificationService.markNotificationAsSent(notification);
            } catch (Exception e) {
                notificationService.handleNotificationFailure(notification, e);
            }
        } finally {
            TenantContext.clear();
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAllocationMoved(AllocationMovedEvent event) {
        RoomAllocation newAllocation = event.newAllocation();
        TenantContext.setTenantId(newAllocation.getTenantId());
        try {
            Room oldRoom = event.oldRoom();
            Room newRoom = newAllocation.getRoom();
            String reason = event.reason();
            LocalDateTime effectiveDate = event.effectiveDate();

            Student student = newAllocation.getStudent();
            AppUser user = student.getAppUser();
            Building newBuilding = newRoom.getBuilding();
            Building oldBuilding = oldRoom.getBuilding();

            String studentName = user.getFirstName() + " " + user.getLastName();
            String email = user.getEmail();

            log.info("Sending allocation moved notification to {} for allocation {}", email, newAllocation.getId());

            Notification notification = notificationService.createNotification(
                    email,
                    EmailSubjects.ALLOCATION_MOVED,
                    NotificationType.ROOM_ALLOCATED,
                    user,
                    newAllocation.getId(),
                    "RoomAllocation",
                    td("allocation-moved", mapOf("studentName", studentName,
                            "newRoomNumber", newRoom.getRoomNumber(),
                            "newBuildingName", newBuilding.getName(),
                            "oldRoomNumber", oldRoom.getRoomNumber(),
                            "oldBuildingName", oldBuilding.getName(), "email", email)));

            try {
                emailDispatchService.sendAllocationMovedEmail(newAllocation, oldRoom, newBuilding, oldBuilding, studentName,
                        email, effectiveDate, reason);
                notificationService.markNotificationAsSent(notification);
            } catch (Exception e) {
                notificationService.handleNotificationFailure(notification, e);
            }
        } finally {
            TenantContext.clear();
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAllocationExpired(AllocationExpiredEvent event) {
        RoomAllocation allocation = event.allocation();
        TenantContext.setTenantId(allocation.getTenantId());
        try {
            boolean isCheckIn = event.isCheckInExpiry();

            if (isCheckIn) {
                sendCheckInExpiredNotificationInternal(allocation);
            } else {
                sendOfferExpiredNotificationInternal(allocation);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void sendOfferExpiredNotificationInternal(RoomAllocation allocation) {
        Student student = allocation.getStudent();
        AppUser user = student.getAppUser();
        Application application = allocation.getApplication();
        Room room = allocation.getRoom();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending offer expired notification to {} for allocation {}", email, allocation.getId());

        Notification notification = notificationService.createNotification(
                email,
                EmailSubjects.OFFER_EXPIRED,
                NotificationType.APPLICATION_REJECTED,
                user,
                allocation.getId(),
                "RoomAllocation",
                td("offer-expired", mapOf("studentName", studentName, "roomNumber", room.getRoomNumber(),
                        "buildingName", room.getBuilding() != null ? room.getBuilding().getName() : "",
                        "applicationCode", application != null ? application.getApplicationCode() : "N/A",
                        "email", email)));

        try {
            emailDispatchService.sendOfferExpiredEmail(allocation, application, room, studentName, email,
                    notification.getSubject());
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationService.handleNotificationFailure(notification, e);
        }
    }

    private void sendCheckInExpiredNotificationInternal(RoomAllocation allocation) {
        Student student = allocation.getStudent();
        AppUser user = student.getAppUser();
        Application application = allocation.getApplication();
        Room room = allocation.getRoom();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending check-in expired notification to {} for allocation {}", email, allocation.getId());

        Notification notification = notificationService.createNotification(
                email,
                EmailSubjects.CHECK_IN_EXPIRED,
                NotificationType.APPLICATION_REJECTED,
                user,
                allocation.getId(),
                "RoomAllocation",
                td("check-in-expired", mapOf("studentName", studentName, "roomNumber", room.getRoomNumber(),
                        "buildingName", room.getBuilding() != null ? room.getBuilding().getName() : "",
                        "applicationCode", application != null ? application.getApplicationCode() : "N/A",
                        "email", email)));

        try {
            emailDispatchService.sendCheckInExpiredEmail(allocation, application, room, studentName, email,
                    notification.getSubject());
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationService.handleNotificationFailure(notification, e);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAllocationAccepted(AllocationAcceptedEvent event) {
        RoomAllocation allocation = event.allocation();
        TenantContext.setTenantId(allocation.getTenantId());
        try {
            log.info("Allocation accepted event received for allocation {}", allocation.getId());
            sendAllocationConfirmedNotification(allocation);
        } finally {
            TenantContext.clear();
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAllocationDeclined(AllocationDeclinedEvent event) {
        RoomAllocation allocation = event.allocation();
        TenantContext.setTenantId(allocation.getTenantId());
        try {
            int totalDeclineCount = event.totalDeclineCount();
            sendOfferDeclinedRequeueNotification(allocation, totalDeclineCount);
        } finally {
            TenantContext.clear();
        }
    }

    private void sendOfferDeclinedRequeueNotification(RoomAllocation allocation, int totalDeclineCount) {
        Student student = allocation.getStudent();
        AppUser user = student.getAppUser();
        Room room = allocation.getRoom();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending offer declined re-queue notification to {} for allocation {}", email, allocation.getId());

        Notification notification = notificationService.createNotification(
                email,
                EmailSubjects.OFFER_DECLINED_REQUEUE,
                NotificationType.ROOM_ALLOCATED,
                user,
                allocation.getId(),
                "RoomAllocation",
                td("offer-declined-requeue", mapOf("studentName", studentName,
                        "roomNumber", room.getRoomNumber(),
                        "buildingName", room.getBuilding() != null ? room.getBuilding().getName() : "",
                        "totalDeclineCount", totalDeclineCount,
                        "declineReason", allocation.getDeclineReason() != null
                                ? allocation.getDeclineReason().getDisplayName() : "Not specified",
                        "email", email)));

        try {
            emailDispatchService.sendOfferDeclinedRequeueEmail(allocation, room, studentName, email, totalDeclineCount,
                    notification.getSubject());
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationService.handleNotificationFailure(notification, e);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStagedAllocationProposed(StagedAllocationProposedEvent event) {
        StagedAllocation stagedAllocation = event.stagedAllocation();
        TenantContext.setTenantId(stagedAllocation.getTenantId());
        try {
            sendAllocationProposalNotification(stagedAllocation);
        } finally {
            TenantContext.clear();
        }
    }

    private void sendAllocationProposalNotification(StagedAllocation stagedAllocation) {
        Student student = stagedAllocation.getStudent();
        AppUser user = student.getAppUser();
        Room room = stagedAllocation.getRoom();
        Building building = room.getBuilding();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending allocation proposal notification to {} for staged allocation {}", email,
                stagedAllocation.getId());

        Notification notification = notificationService.createNotification(
                email,
                EmailSubjects.ALLOCATION_PROPOSAL,
                NotificationType.ROOM_ALLOCATED,
                user,
                stagedAllocation.getId(),
                "StagedAllocation",
                td("allocation-proposal", mapOf("studentName", studentName,
                        "roomNumber", room.getRoomNumber(),
                        "buildingName", building.getName(), "proposalId", stagedAllocation.getId(),
                        "email", email)));

        try {
            emailDispatchService.sendAllocationProposalEmail(stagedAllocation, room, building, studentName, email);
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationService.handleNotificationFailure(notification, e);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStagedAllocationConfirmed(StagedAllocationConfirmedEvent event) {
        RoomAllocation roomAllocation = event.roomAllocation();
        if (roomAllocation != null) {
            TenantContext.setTenantId(roomAllocation.getTenantId());
            try {
                sendAllocationConfirmedNotification(roomAllocation);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void sendAllocationConfirmedNotification(RoomAllocation allocation) {
        Student student = allocation.getStudent();
        AppUser user = student.getAppUser();
        Room room = allocation.getRoom();
        Building building = room.getBuilding();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending allocation confirmed notification to {} for allocation {}", email, allocation.getId());

        Notification notification = notificationService.createNotification(
                email,
                EmailSubjects.ALLOCATION_CONFIRMED,
                NotificationType.ROOM_ALLOCATED,
                user,
                allocation.getId(),
                "RoomAllocation",
                td("allocation-confirmed", mapOf("studentName", studentName,
                        "roomNumber", room.getRoomNumber(),
                        "buildingName", building.getName(), "allocationId", allocation.getId(),
                        "email", email)));

        try {
            emailDispatchService.sendAllocationConfirmedEmail(allocation, room, building, studentName, email);
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationService.handleNotificationFailure(notification, e);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStagedAllocationRejected(StagedAllocationRejectedEvent event) {
        StagedAllocation stagedAllocation = event.stagedAllocation();
        TenantContext.setTenantId(stagedAllocation.getTenantId());
        try {
            sendAllocationRejectedNotification(stagedAllocation, event.reason(), 0);
        } finally {
            TenantContext.clear();
        }
    }

    private void sendAllocationRejectedNotification(StagedAllocation stagedAllocation, String reason,
            Integer waitingListPosition) {
        Student student = stagedAllocation.getStudent();
        AppUser user = student.getAppUser();
        Room room = stagedAllocation.getRoom();
        Building building = room.getBuilding();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending allocation rejected notification to {} for staged allocation {}", email,
                stagedAllocation.getId());

        Notification notification = notificationService.createNotification(
                email,
                EmailSubjects.ALLOCATION_REJECTED,
                NotificationType.APPLICATION_REJECTED,
                user,
                stagedAllocation.getId(),
                "StagedAllocation",
                td("allocation-rejected", mapOf("studentName", studentName,
                        "roomNumber", room.getRoomNumber(),
                        "buildingName", building.getName(), "proposalId", stagedAllocation.getId(),
                        "reason", reason != null ? reason : "",
                        "waitingListPosition", waitingListPosition != null ? waitingListPosition : 0,
                        "email", email)));

        try {
            emailDispatchService.sendAllocationRejectedEmail(stagedAllocation, room, building, studentName, email,
                    reason, waitingListPosition);
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationService.handleNotificationFailure(notification, e);
        }
    }
}
