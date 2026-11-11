package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.model.event.ApplicationsWithdrawnEvent;
import com.beezhub.student_accommodation.model.event.ApplicationsWithdrawnEvent.Withdrawn;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tells everyone affected when a student's overlapping applications were withdrawn on another
 * acceptance (GH-497): one email to the student listing what was withdrawn, and an in-app
 * notification to each affected company's admins about their own applications only. Sent after the
 * withdrawal has committed, never inside it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalNotificationListener {

    static final String TEMPLATE = "applications-withdrawn";
    static final String COMPANY_NOTIFICATION_TYPE = "APPLICATION_WITHDRAWN";

    private final NotificationPersistenceService notificationService;
    private final EmailService emailService;
    private final UserNotificationService userNotificationService;
    private final AppUserRepository appUserRepository;
    private final EntityManager entityManager;
    private final MeterRegistry meterRegistry;

    @Value("${app.base-url}")
    private String baseUrl;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleApplicationsWithdrawn(ApplicationsWithdrawnEvent event) {
        try {
            TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
            emailStudent(event);
            Map<Long, List<Withdrawn>> byCompany = event.withdrawn().stream()
                    .filter(w -> w.tenantId() != null)
                    .collect(Collectors.groupingBy(Withdrawn::tenantId, LinkedHashMap::new, Collectors.toList()));
            byCompany.forEach(this::notifyCompany);
        } finally {
            TenantContext.clear();
        }
    }

    private void emailStudent(ApplicationsWithdrawnEvent event) {
        Student student = entityManager.find(Student.class, event.studentId());
        if (student == null || student.getAppUser() == null) {
            log.warn("action=withdrawalEmailSkipped reason=studentMissing studentId={}", event.studentId());
            return;
        }
        AppUser user = student.getAppUser();
        TemplatedEmail email = new TemplatedEmail(TEMPLATE, EmailSubjects.APPLICATIONS_WITHDRAWN,
                new ApplicationsWithdrawnContext(
                        user.getFirstName() + " " + user.getLastName(),
                        acceptedPlace(event.acceptedAllocationId()),
                        event.withdrawn().stream().map(WithdrawalNotificationListener::line).toList(),
                        baseUrl + "/applications"));
        Notification notification = notificationService.createNotification(user.getEmail(), email.subject(),
                NotificationType.GENERAL, user, event.acceptedAllocationId(), "RoomAllocation", email.templateData());
        try {
            emailService.sendTemplateEmail(user.getEmail(), email.subject(), email.templateName(), email.context());
            meterRegistry.counter("sas.emails.sent").increment();
            notificationService.markNotificationAsSent(notification);
        } catch (Exception e) {
            meterRegistry.counter("sas.emails.failed").increment();
            notificationService.handleNotificationFailure(notification, e);
        }
    }

    /** Each company hears about its own applications only, never where the student accepted. */
    private void notifyCompany(Long tenantId, List<Withdrawn> withdrawn) {
        TenantContext.setTenantId(tenantId);
        try {
            List<AppUser> admins = appUserRepository.findByTenantIdAndUserRole(tenantId, UserRole.TENANT_ADMIN);
            for (Withdrawn item : withdrawn) {
                String message = "Application " + item.applicationCode()
                        + (item.intakeName() != null ? " for " + item.intakeName() : "")
                        + " was withdrawn: the student accepted a place elsewhere for overlapping dates."
                        + (item.offersCancelled() > 0 ? " Their room offer was cancelled and the bed released." : "");
                for (AppUser admin : admins) {
                    userNotificationService.createNotification(admin.getId(), COMPANY_NOTIFICATION_TYPE,
                            "Application withdrawn", message, Map.of("applicationId", item.applicationId()));
                }
            }
            log.info("action=companyNotifiedOfWithdrawal tenantId={} applications={} admins={}",
                    tenantId, withdrawn.size(), admins.size());
        } catch (Exception e) {
            log.error("action=companyWithdrawalNotificationFailed tenantId={}", tenantId, e);
        } finally {
            TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        }
    }

    private String acceptedPlace(Long allocationId) {
        RoomAllocation allocation = allocationId == null ? null : entityManager.find(RoomAllocation.class, allocationId);
        Room room = allocation != null ? allocation.getRoom() : null;
        if (room == null) {
            return "a place";
        }
        String building = room.getBuilding() != null ? ", " + room.getBuilding().getName() : "";
        return "Room " + room.getRoomNumber() + building;
    }

    private static String line(Withdrawn withdrawn) {
        return withdrawn.applicationCode()
                + (withdrawn.companyName() != null ? " at " + withdrawn.companyName() : "")
                + (withdrawn.intakeName() != null ? ", " + withdrawn.intakeName() : "");
    }
}
