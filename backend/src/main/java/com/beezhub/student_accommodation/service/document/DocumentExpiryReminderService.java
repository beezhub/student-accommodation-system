package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.config.TransactionTemplateConfig;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.event.DocumentExpiryReminderEvent;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.CompanyRules;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reminds students to replace an expiring document (GH-498), daily, one company at a time.
 *
 * <p>A student is reminded by a company while they have an application in progress there, or an
 * offer, an accepted place or a room there. The first reminder goes out
 * {@code document_expiry_reminder_days} before expiry and then every {@value #INTERVAL_DAYS} days
 * until it expires; after expiry, the day after and then every {@value #INTERVAL_DAYS} days until the
 * document is replaced. The cadence is worked out from the dates. The same document expiring on the
 * same day at two companies is reminded once.
 *
 * <p>Each reminder is recorded on the document ({@code expiry_reminded_at}), and a document already
 * reminded today by an earlier run, or by another instance, is skipped, so running the job twice in
 * a day sends nothing new (GH-514).
 *
 * <p>The job sets the tenant context per company, since scheduled code otherwise sees every company,
 * and publishes each reminder inside a transaction so the email is sent after commit, never inside
 * the job.
 */
@Service
@Slf4j
public class DocumentExpiryReminderService {

    static final int INTERVAL_DAYS = 7;

    /** Applications still in progress. CHECKED_IN is left out: a resident is reached through the room. */
    static final Set<ApplicationStatus> IN_PROGRESS = EnumSet.of(ApplicationStatus.PENDING,
            ApplicationStatus.UNDER_REVIEW, ApplicationStatus.CHANGES_REQUESTED, ApplicationStatus.APPROVED,
            ApplicationStatus.AWAITING_ALLOCATION, ApplicationStatus.ALLOCATED);

    static final Set<AllocationStatus> HOLDING_A_BED = EnumSet.of(AllocationStatus.OFFERED,
            AllocationStatus.ACCEPTED, AllocationStatus.ACTIVE);

    /** Which reminder, if any, is due today. */
    enum Reminder { NONE, EXPIRES_SOON, EXPIRED }

    private final ApplicationRepository applicationRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentExpiryPolicy documentExpiryPolicy;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public DocumentExpiryReminderService(
            ApplicationRepository applicationRepository,
            RoomAllocationRepository roomAllocationRepository,
            DocumentRepository documentRepository,
            DocumentExpiryPolicy documentExpiryPolicy,
            ApplicationEventPublisher eventPublisher,
            @Qualifier(TransactionTemplateConfig.REQUIRES_NEW) TransactionTemplate transactionTemplate,
            Clock clock) {
        this.applicationRepository = applicationRepository;
        this.roomAllocationRepository = roomAllocationRepository;
        this.documentRepository = documentRepository;
        this.documentExpiryPolicy = documentExpiryPolicy;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * Daily job body, called by {@code ScheduledJobRunner}.
     *
     * @return how many reminders were sent
     */
    public int runDaily() {
        Set<Long> companies = new LinkedHashSet<>(applicationRepository.findTenantIdsWithStatusIn(IN_PROGRESS));
        companies.addAll(roomAllocationRepository.findTenantIdsWithStatusIn(HOLDING_A_BED));
        Set<String> sent = new HashSet<>();
        Set<Long> remindedThisRun = new HashSet<>();
        Long previousTenant = TenantContext.getTenantId();
        int reminded = 0;
        try {
            for (Long tenantId : companies) {
                if (tenantId == null || TenantContext.PLATFORM_TENANT_ID.equals(tenantId)) {
                    continue;
                }
                TenantContext.setTenantId(tenantId);
                try {
                    Set<Long> recorded = new HashSet<>();
                    Integer count = transactionTemplate.execute(
                            status -> remindCompany(tenantId, sent, remindedThisRun, recorded));
                    reminded += count == null ? 0 : count;
                    // Added only once the company's transaction has gone through: a record that was
                    // rolled back is not on the document, so a later company has to make it again.
                    remindedThisRun.addAll(recorded);
                } catch (Exception e) {
                    log.error("action=documentExpiryReminderFailed tenantId={}", tenantId, e);
                }
            }
        } finally {
            if (previousTenant == null) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(previousTenant);
            }
        }
        return reminded;
    }

    /**
     * Reminders for one company, inside its own transaction. {@code remindedThisRun} holds the
     * documents that earlier companies in this run recorded a reminder on, and {@code recorded}
     * collects the ones this company records.
     */
    int remindCompany(Long tenantId, Set<String> sent, Set<Long> remindedThisRun, Set<Long> recorded) {
        CompanyRules rules = documentExpiryPolicy.forCompany(tenantId);
        if (!rules.hasExpiringTypes()) {
            return 0;
        }
        Map<Long, Long> institutionByStudent = studentsOf(tenantId);
        if (institutionByStudent.isEmpty()) {
            return 0;
        }
        int reminded = 0;
        LocalDateTime now = LocalDateTime.now(clock);
        for (Document document : documentRepository.findAllByStudent_IdIn(new ArrayList<>(institutionByStudent.keySet()))) {
            Long institutionId = institutionByStudent.get(document.getStudent().getId());
            Optional<LocalDate> expiry = rules.expiryOf(document, institutionId);
            if (expiry.isEmpty()) {
                continue;
            }
            Reminder due = reminderDue(rules.today(), expiry.get(), rules.reminderDays());
            if (due == Reminder.NONE || !sent.add(document.getId() + "|" + expiry.get() + "|" + due)) {
                continue;
            }
            if (!recordReminder(document, rules.today(), now, remindedThisRun)) {
                log.info("action=documentExpiryReminderSkipped reason=alreadyRemindedToday documentId={} tenantId={}",
                        document.getId(), tenantId);
                continue;
            }
            recorded.add(document.getId());
            eventPublisher.publishEvent(new DocumentExpiryReminderEvent(document.getId(), tenantId, expiry.get(),
                    due == Reminder.EXPIRED));
            reminded++;
        }
        log.info("action=documentExpiryReminders tenantId={} students={} reminded={}",
                tenantId, institutionByStudent.size(), reminded);
        return reminded;
    }

    /**
     * Records the reminder on the document, and says whether it may be sent (GH-514). False when an
     * earlier run today, or another instance, already reminded this document. A document recorded
     * earlier in this run by another company is not recorded again and may still be sent, so two
     * companies with different expiry dates each send their own reminder, as before.
     */
    private boolean recordReminder(Document document, LocalDate today, LocalDateTime now, Set<Long> remindedThisRun) {
        return remindedThisRun.contains(document.getId())
                || documentRepository.markExpiryReminded(document.getId(), now, today.atStartOfDay()) == 1;
    }

    /** The company's students, with the institution their application there is for. */
    private Map<Long, Long> studentsOf(Long tenantId) {
        Map<Long, Long> institutionByStudent = new HashMap<>();
        for (Application application : applicationRepository.findByTenantIdAndStatusInWithStudent(tenantId, IN_PROGRESS)) {
            institutionByStudent.put(application.getStudent().getId(),
                    DocumentExpiryPolicy.institutionOf(application, application.getStudent()));
        }
        for (RoomAllocation allocation : roomAllocationRepository.findByTenantIdAndStatusInWithStudent(tenantId, HOLDING_A_BED)) {
            institutionByStudent.putIfAbsent(allocation.getStudent().getId(),
                    DocumentExpiryPolicy.institutionOf(allocation.getApplication(), allocation.getStudent()));
        }
        return institutionByStudent;
    }

    /**
     * The reminder due on {@code today} for a document valid up to {@code expiry}: on the first day
     * of the reminder period and every {@value #INTERVAL_DAYS} days after, then the day after expiry
     * and every {@value #INTERVAL_DAYS} days after that.
     */
    static Reminder reminderDue(LocalDate today, LocalDate expiry, int reminderDays) {
        long daysLeft = ChronoUnit.DAYS.between(today, expiry);
        if (daysLeft >= 0) {
            long intoPeriod = reminderDays - daysLeft;
            return intoPeriod >= 0 && intoPeriod % INTERVAL_DAYS == 0 ? Reminder.EXPIRES_SOON : Reminder.NONE;
        }
        return (-daysLeft - 1) % INTERVAL_DAYS == 0 ? Reminder.EXPIRED : Reminder.NONE;
    }
}
