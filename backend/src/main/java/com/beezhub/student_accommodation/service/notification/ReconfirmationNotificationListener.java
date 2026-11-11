package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.model.event.PlaceReleasedEvent;
import com.beezhub.student_accommodation.model.event.ReconfirmationRequestedEvent;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emails for reconfirmation before move-in (GH-496), sent after the daily job's change has
 * committed and never inside its transaction. Each email is stored as a {@link Notification} first,
 * so a failed send is retried by {@code NotificationRetryService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReconfirmationNotificationListener {

    private final NotificationPersistenceService notificationService;
    private final ReconfirmationEmailService reconfirmationEmailService;
    private final EntityManager entityManager;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReconfirmationRequested(ReconfirmationRequestedEvent event) {
        RoomAllocation allocation = reload(event.allocation());
        TenantContext.setTenantId(allocation.getTenantId());
        try {
            AppUser user = allocation.getStudent().getAppUser();
            send(allocation, user, NotificationType.GENERAL, reconfirmationEmailService.request(
                    allocation, event.reminder(), event.releaseDate(), nameOf(user)));
        } finally {
            TenantContext.clear();
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePlaceReleased(PlaceReleasedEvent event) {
        RoomAllocation allocation = reload(event.allocation());
        TenantContext.setTenantId(allocation.getTenantId());
        try {
            AppUser user = allocation.getStudent().getAppUser();
            send(allocation, user, NotificationType.APPLICATION_REJECTED,
                    reconfirmationEmailService.released(allocation, nameOf(user)));
        } finally {
            TenantContext.clear();
        }
    }

    private void send(RoomAllocation allocation, AppUser user, NotificationType type,
            ReconfirmationEmailService.Email email) {
        Notification notification = notificationService.createNotification(user.getEmail(), email.subject(),
                type, user, allocation.getId(), "RoomAllocation", email.templateData());
        try {
            reconfirmationEmailService.send(user.getEmail(), email);
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationService.handleNotificationFailure(notification, e);
        }
    }

    /** The event carries a detached entity; its lazy associations need this session (GH-131). */
    private RoomAllocation reload(RoomAllocation detached) {
        RoomAllocation managed = entityManager.find(RoomAllocation.class, detached.getId());
        return managed != null ? managed : detached;
    }

    private static String nameOf(AppUser user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
