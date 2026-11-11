package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.model.event.StayExtendedEvent;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.service.EmailService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

/**
 * Tells the student and the company when a stay was extended (GH-498), after the extension has
 * committed and never inside it: a confirmation email to the student, stored first so a failed send
 * is retried, and an in-app notification to the company's admins, who can still end the stay.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StayExtensionNotificationListener {

    static final String TEMPLATE = "stay-extended";
    static final String COMPANY_NOTIFICATION_TYPE = "STAY_EXTENDED";

    private final NotificationPersistenceService notificationService;
    private final EmailService emailService;
    private final EmailContentBuilder contentBuilder;
    private final UserNotificationService userNotificationService;
    private final AppUserRepository appUserRepository;
    private final EntityManager entityManager;
    private final MeterRegistry meterRegistry;

    @Value("${app.base-url}")
    private String baseUrl;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStayExtended(StayExtendedEvent event) {
        RoomAllocation allocation = entityManager.find(RoomAllocation.class, event.allocationId());
        if (allocation == null || allocation.getStudent() == null || allocation.getStudent().getAppUser() == null) {
            log.warn("action=stayExtendedNoticeSkipped reason=allocationMissing allocationId={}", event.allocationId());
            return;
        }
        TenantContext.setTenantId(allocation.getTenantId());
        try {
            AppUser student = allocation.getStudent().getAppUser();
            String studentName = student.getFirstName() + " " + student.getLastName();
            emailStudent(allocation, student, studentName, event);
            notifyCompany(allocation, studentName, event);
        } finally {
            TenantContext.clear();
        }
    }

    private void emailStudent(RoomAllocation allocation, AppUser student, String studentName, StayExtendedEvent event) {
        Room room = allocation.getRoom();
        TemplatedEmail email = new TemplatedEmail(TEMPLATE, EmailSubjects.STAY_EXTENDED, new StayExtendedContext(
                studentName, room.getRoomNumber(), buildingName(room), event.intakeName(),
                contentBuilder.formatDate(event.stayEndDate()), baseUrl + "/dashboard"));
        Notification notification = notificationService.createNotification(student.getEmail(), email.subject(),
                NotificationType.GENERAL, student, allocation.getId(), "RoomAllocation", email.templateData());
        try {
            emailService.sendTemplateEmail(student.getEmail(), email.subject(), email.templateName(), email.context());
            meterRegistry.counter("sas.emails.sent").increment();
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            meterRegistry.counter("sas.emails.failed").increment();
            notificationService.handleNotificationFailure(notification, e);
        }
    }

    /** The company's admins hear about it in the app; they can still end the stay. */
    private void notifyCompany(RoomAllocation allocation, String studentName, StayExtendedEvent event) {
        try {
            List<AppUser> admins = appUserRepository.findByTenantIdAndUserRole(allocation.getTenantId(),
                    UserRole.TENANT_ADMIN);
            String message = studentName + " is staying on in room " + allocation.getRoom().getRoomNumber()
                    + " for " + event.intakeName() + ", until " + contentBuilder.formatDate(event.stayEndDate()) + ".";
            for (AppUser admin : admins) {
                userNotificationService.createNotification(admin.getId(), COMPANY_NOTIFICATION_TYPE,
                        "Stay extended", message, Map.of("allocationId", allocation.getId()));
            }
            log.info("action=companyNotifiedOfStayExtension allocationId={} admins={}", allocation.getId(), admins.size());
        } catch (Exception e) {
            log.error("action=companyStayExtensionNotificationFailed allocationId={}", allocation.getId(), e);
        }
    }

    private static String buildingName(Room room) {
        return room.getBuilding() != null ? room.getBuilding().getName() : "your building";
    }
}
