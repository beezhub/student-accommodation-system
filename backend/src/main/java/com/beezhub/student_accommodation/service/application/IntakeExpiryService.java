package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.config.TransactionTemplateConfig;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.event.ApplicationStatusChangedEvent;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.service.allocation.ApplicationStatusValidator;
import com.beezhub.student_accommodation.service.allocation.StayPeriod;
import com.beezhub.student_accommodation.service.allocation.WaitingListService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Expires unfinished applications once their intake's stay is over (GH-497), so no application
 * waits for a term that has ended.
 *
 * <p>The day after the stay ends (for an Annual intake, the year end), PENDING, UNDER_REVIEW,
 * CHANGES_REQUESTED, APPROVED and AWAITING_ALLOCATION applications move to EXPIRED and their
 * waiting-list entries close. {@link ApplicationStatusChangedEvent} is published, so the student is
 * emailed after commit with the next open intake at that company, if one is open.
 *
 * <p>An application with an open or accepted offer is left alone: offer expiry and check-in expiry
 * already end those. The daily job sets the tenant context per company, since scheduled code
 * otherwise sees every company, and expires each application in its own transaction, so one
 * failure costs one application.
 */
@Service
@Slf4j
public class IntakeExpiryService {

    /** The statuses an application can still be in while nobody has finished it. */
    static final Set<ApplicationStatus> UNFINISHED = EnumSet.of(
            ApplicationStatus.PENDING,
            ApplicationStatus.UNDER_REVIEW,
            ApplicationStatus.CHANGES_REQUESTED,
            ApplicationStatus.APPROVED,
            ApplicationStatus.AWAITING_ALLOCATION);

    private static final Set<AllocationStatus> OFFER_STATUSES =
            EnumSet.of(AllocationStatus.OFFERED, AllocationStatus.ACCEPTED);

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMMM yyyy");

    private final ApplicationRepository applicationRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final WaitingListService waitingListService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public IntakeExpiryService(
            ApplicationRepository applicationRepository,
            RoomAllocationRepository roomAllocationRepository,
            WaitingListService waitingListService,
            ApplicationEventPublisher eventPublisher,
            @Qualifier(TransactionTemplateConfig.REQUIRES_NEW) TransactionTemplate transactionTemplate,
            Clock clock) {
        this.applicationRepository = applicationRepository;
        this.roomAllocationRepository = roomAllocationRepository;
        this.waitingListService = waitingListService;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * Daily job body, called by {@code ScheduledJobRunner}: one company at a time.
     *
     * @return how many applications expired
     */
    public int runDaily() {
        LocalDate today = LocalDate.now(clock);
        Long previousTenant = TenantContext.getTenantId();
        int expired = 0;
        try {
            for (Long tenantId : applicationRepository.findTenantIdsWithStayEndedBefore(UNFINISHED, today)) {
                if (tenantId == null) {
                    continue;
                }
                TenantContext.setTenantId(tenantId);
                expired += runForCompany(tenantId, today);
            }
        } finally {
            if (previousTenant == null) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(previousTenant);
            }
        }
        return expired;
    }

    private int runForCompany(Long tenantId, LocalDate today) {
        List<Long> ids = applicationRepository.findIdsWithStayEndedBefore(tenantId, UNFINISHED, today);
        int expired = 0;
        for (Long id : ids) {
            try {
                if (Boolean.TRUE.equals(transactionTemplate.execute(status -> expire(id, today)))) {
                    expired++;
                }
            } catch (Exception e) {
                log.error("action=intakeExpiryFailed applicationId={} tenantId={}", id, tenantId, e);
            }
        }
        log.info("action=intakeExpiryRun tenantId={} candidates={} expired={}", tenantId, ids.size(), expired);
        return expired;
    }

    /** Expires one application, inside its own transaction. Re-checks what the query selected. */
    boolean expire(Long applicationId, LocalDate today) {
        Application application = applicationRepository.findWithDetailsById(applicationId).orElse(null);
        if (application == null || !UNFINISHED.contains(application.getStatus())) {
            return false;
        }
        StayPeriod stay = StayPeriod.of(application);
        if (stay.end() == null || !stay.end().isBefore(today)) {
            return false;
        }
        if (!roomAllocationRepository.findByApplicationIdAndStatusIn(applicationId, OFFER_STATUSES).isEmpty()) {
            log.info("action=intakeExpirySkipped reason=offerOutstanding applicationId={}", applicationId);
            return false;
        }

        ApplicationStatus previous = application.getStatus();
        ApplicationStatusValidator.validate(application, ApplicationStatus.EXPIRED);
        application.setStatus(ApplicationStatus.EXPIRED);
        application.setRemarks("Expired: the stay applied for ended on " + stay.end().format(DAY) + ".");
        Application saved = applicationRepository.save(application);
        waitingListService.closeEntriesForApplication(applicationId);

        eventPublisher.publishEvent(new ApplicationStatusChangedEvent(saved, previous, ApplicationStatus.EXPIRED));
        log.info("action=applicationExpired applicationId={} from={} stayEnd={}", applicationId, previous, stay.end());
        return true;
    }
}
