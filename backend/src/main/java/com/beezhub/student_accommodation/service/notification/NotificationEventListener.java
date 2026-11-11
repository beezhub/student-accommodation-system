package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.*;
import com.beezhub.student_accommodation.model.event.*;
import com.beezhub.student_accommodation.repository.NotificationRepository;
import com.beezhub.student_accommodation.service.allocation.WaitingListService;
import com.beezhub.student_accommodation.service.notification.NotificationPersistenceService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationPersistenceService notificationService;
    private final EmailDispatchService emailDispatchService;
    private final ApplicationExpiryEmailService applicationExpiryEmailService;

    /** The shared, transaction-bound EntityManager proxy; see {@link #reload(Application)}. */
    private final EntityManager entityManager;

    /**
     * Re-loads an entity carried on an event so its LAZY associations can be read.
     *
     * <p>Every handler here runs on the notification executor, after the publishing transaction has
     * committed. The entity on the event is detached and its proxies are bound to a closed session,
     * so the first {@code getStudent().getAppUser()} threw {@code LazyInitializationException}
     * before the inner try block. No {@code Notification} row was written and nothing was retried,
     * so the email was lost with only a log line (GH-131).
     *
     * <p>Each handler is {@code REQUIRES_NEW}, which gives the async thread its own session; these
     * helpers return an instance managed by it. Falling back to the detached entity keeps the old
     * behaviour when the row has since been deleted, rather than turning a lost email into a crash.
     */
    private Application reload(Application detached) {
        return detached == null ? null : orSelf(entityManager.find(Application.class, detached.getId()), detached);
    }

    private Document reload(Document detached) {
        return detached == null ? null : orSelf(entityManager.find(Document.class, detached.getId()), detached);
    }

    private Room reload(Room detached) {
        return detached == null ? null : orSelf(entityManager.find(Room.class, detached.getId()), detached);
    }

    private RoomAllocation reload(RoomAllocation detached) {
        return detached == null ? null : orSelf(entityManager.find(RoomAllocation.class, detached.getId()), detached);
    }

    private StagedAllocation reload(StagedAllocation detached) {
        return detached == null ? null
                : orSelf(entityManager.find(StagedAllocation.class, detached.getId()), detached);
    }

    private <T> T orSelf(T managed, T detached) {
        if (managed == null) {
            log.warn("action=notificationEntityMissing type={} — sending from the detached copy",
                    detached.getClass().getSimpleName());
            return detached;
        }
        return managed;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        Application application = reload(event.application());
        TenantContext.setTenantId(application.getTenantId());
        try {
            ApplicationStatus newStatus = event.newStatus();

            Student student = application.getStudent();
            AppUser user = student.getAppUser();
            String studentName = user.getFirstName() + " " + user.getLastName();
            String email = user.getEmail();

            log.info("Sending application status notification to {} for application {}, status: {}",
                    email, application.getId(), newStatus);

            if (newStatus == ApplicationStatus.CHANGES_REQUESTED) {
                sendChangesRequestedNotification(application, user, studentName, email);
                return;
            }
            if (newStatus == ApplicationStatus.EXPIRED) {
                sendExpiredNotification(application, user, studentName, email);
                return;
            }

            NotificationType notificationType = newStatus == ApplicationStatus.APPROVED
                    ? NotificationType.APPLICATION_APPROVED
                    : NotificationType.APPLICATION_REJECTED;

            String subject = newStatus == ApplicationStatus.APPROVED
                    ? EmailSubjects.APPLICATION_APPROVED
                    : EmailSubjects.APPLICATION_REJECTED;

            Notification notification = notificationService.createNotification(
                    email, subject, notificationType, user,
                    application.getId(), "Application",
                    newStatus == ApplicationStatus.APPROVED
                            ? emailDispatchService.applicationApprovedTemplateData(application, studentName)
                            : emailDispatchService.applicationRejectedTemplateData(application, studentName));

            try {
                switch (newStatus) {
                    case APPROVED -> emailDispatchService.sendApplicationApprovedEmail(application, studentName, email);
                    case REJECTED -> emailDispatchService.sendApplicationRejectedEmail(application, studentName, email);
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

    /**
     * The stored template data carries everything the template prints, because the retry job
     * rebuilds the email from this row alone.
     */
    private void sendChangesRequestedNotification(Application application, AppUser user, String studentName,
            String email) {
        Notification notification = notificationService.createNotification(
                email,
                EmailSubjects.APPLICATION_CHANGES_REQUESTED,
                NotificationType.APPLICATION_CHANGES_REQUESTED,
                user,
                application.getId(),
                "Application",
                emailDispatchService.applicationChangesRequestedTemplateData(application, studentName));

        try {
            emailDispatchService.sendApplicationChangesRequestedEmail(application, studentName, email);
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationService.handleNotificationFailure(notification, e);
        }
    }

    /** Expired with its intake (GH-497): the email offers the next open intake at that company. */
    private void sendExpiredNotification(Application application, AppUser user, String studentName, String email) {
        TemplatedEmail expired = applicationExpiryEmailService.expired(application, studentName);
        Notification notification = notificationService.createNotification(email, expired.subject(),
                NotificationType.GENERAL, user, application.getId(), "Application", expired.templateData());
        try {
            applicationExpiryEmailService.send(email, expired);
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationService.handleNotificationFailure(notification, e);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleDocumentVerification(DocumentVerificationEvent event) {
        Document document = reload(event.document());
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
                    newStatus == DocumentStatus.APPROVED
                            ? emailDispatchService.documentVerifiedTemplateData(document, studentName, documentType)
                            : emailDispatchService.documentRejectedTemplateData(document, studentName, documentType, comment));

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleDocumentComment(DocumentCommentEvent event) {
        Document document = reload(event.document());
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
                    emailDispatchService.documentCommentTemplateData(document, commentText, "Accommodation Office",
                            studentName, documentName, documentType));

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAllocationCreated(AllocationCreatedEvent event) {
        RoomAllocation allocation = reload(event.allocation());
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
                    emailDispatchService.roomAllocatedTemplateData(allocation, room, building, studentName));

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAllocationMoved(AllocationMovedEvent event) {
        RoomAllocation newAllocation = reload(event.newAllocation());
        TenantContext.setTenantId(newAllocation.getTenantId());
        try {
            Room oldRoom = reload(event.oldRoom());
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
                    emailDispatchService.allocationMovedTemplateData(newAllocation, oldRoom, newBuilding, oldBuilding,
                            studentName, effectiveDate, reason));

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAllocationExpired(AllocationExpiredEvent event) {
        RoomAllocation allocation = reload(event.allocation());
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
                emailDispatchService.offerExpiredTemplateData(application, room, studentName));

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
                emailDispatchService.checkInExpiredTemplateData(application, room, studentName));

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAllocationAccepted(AllocationAcceptedEvent event) {
        RoomAllocation allocation = reload(event.allocation());
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAllocationDeclined(AllocationDeclinedEvent event) {
        RoomAllocation allocation = reload(event.allocation());
        TenantContext.setTenantId(allocation.getTenantId());
        try {
            int totalDeclineCount = event.totalDeclineCount();
            if (totalDeclineCount >= WaitingListService.MAX_DECLINES) {
                sendOfferDeclinedMaxReachedNotification(allocation, totalDeclineCount);
            } else {
                sendOfferDeclinedRequeueNotification(allocation, totalDeclineCount);
            }
        } finally {
            TenantContext.clear();
        }
    }

    /** The final decline closes the application, so the student is told that, not that they re-queued. */
    private void sendOfferDeclinedMaxReachedNotification(RoomAllocation allocation, int totalDeclineCount) {
        Student student = allocation.getStudent();
        AppUser user = student.getAppUser();
        Application application = allocation.getApplication();

        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending offer declined max reached notification to {} for allocation {}", email, allocation.getId());

        Notification notification = notificationService.createNotification(
                email,
                EmailSubjects.OFFER_DECLINED_MAX_REACHED,
                NotificationType.APPLICATION_REJECTED,
                user,
                allocation.getId(),
                "RoomAllocation",
                emailDispatchService.offerDeclinedMaxReachedTemplateData(application, studentName, totalDeclineCount));

        try {
            emailDispatchService.sendOfferDeclinedMaxReachedEmail(allocation, application, studentName, email,
                    totalDeclineCount, notification.getSubject());
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationService.handleNotificationFailure(notification, e);
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
                emailDispatchService.offerDeclinedRequeueTemplateData(allocation, room, studentName, totalDeclineCount));

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStagedAllocationProposed(StagedAllocationProposedEvent event) {
        StagedAllocation stagedAllocation = reload(event.stagedAllocation());
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
                emailDispatchService.allocationProposalTemplateData(stagedAllocation, room, building, studentName));

        try {
            emailDispatchService.sendAllocationProposalEmail(stagedAllocation, room, building, studentName, email);
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationService.handleNotificationFailure(notification, e);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStagedAllocationConfirmed(StagedAllocationConfirmedEvent event) {
        RoomAllocation roomAllocation = reload(event.roomAllocation());
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
                emailDispatchService.allocationConfirmedTemplateData(allocation, room, building, studentName));

        try {
            emailDispatchService.sendAllocationConfirmedEmail(allocation, room, building, studentName, email);
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationService.handleNotificationFailure(notification, e);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStagedAllocationRejected(StagedAllocationRejectedEvent event) {
        StagedAllocation stagedAllocation = reload(event.stagedAllocation());
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
                emailDispatchService.allocationRejectedTemplateData(stagedAllocation, room, building, studentName,
                        reason, waitingListPosition));

        try {
            emailDispatchService.sendAllocationRejectedEmail(stagedAllocation, room, building, studentName, email,
                    reason, waitingListPosition);
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationService.handleNotificationFailure(notification, e);
        }
    }
}
