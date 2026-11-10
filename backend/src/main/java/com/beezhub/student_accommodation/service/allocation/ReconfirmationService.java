package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.config.TransactionTemplateConfig;
import com.beezhub.student_accommodation.exceptions.InvalidAllocationStatusTransitionException;
import com.beezhub.student_accommodation.exceptions.RoomAllocationNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.event.PlaceReleasedEvent;
import com.beezhub.student_accommodation.model.event.ReconfirmationRequestedEvent;
import com.beezhub.student_accommodation.model.event.RoomVacatedEvent;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * "Still coming?" before move-in (GH-496).
 *
 * <p>A place accepted more than {@code reconfirm_min_lead_days} before move-in is asked to
 * reconfirm {@code reconfirm_ask_days_before} move-in, reminded every {@value #REMINDER_INTERVAL_DAYS}
 * days, and released {@code reconfirm_release_days_before} move-in if the student has not answered:
 * ACCEPTED to CANCELLED, the application cancelled, and {@link RoomVacatedEvent} published so
 * {@code WaitingListRefillListener} offers the bed to the next student.
 *
 * <p>The daily job sets the tenant context per company, since scheduled code otherwise sees every
 * company, and handles each allocation in its own transaction, so one failure costs one place.
 */
@Service
@Slf4j
public class ReconfirmationService {

    static final int REMINDER_INTERVAL_DAYS = 7;

    /** What the daily job did with one allocation. */
    enum Step { NONE, ASKED, REMINDED, RELEASED }

    private final RoomAllocationRepository roomAllocationRepository;
    private final ApplicationRepository applicationRepository;
    private final ReconfirmationPolicy reconfirmationPolicy;
    private final RoomOccupancyManager roomOccupancyManager;
    private final AllocationAuditService allocationAuditService;
    private final WaitingListService waitingListService;
    private final AllocationAccessGuard allocationAccessGuard;
    private final RoomAllocationMapper roomAllocationMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public ReconfirmationService(
            RoomAllocationRepository roomAllocationRepository,
            ApplicationRepository applicationRepository,
            ReconfirmationPolicy reconfirmationPolicy,
            RoomOccupancyManager roomOccupancyManager,
            AllocationAuditService allocationAuditService,
            WaitingListService waitingListService,
            AllocationAccessGuard allocationAccessGuard,
            RoomAllocationMapper roomAllocationMapper,
            ApplicationEventPublisher eventPublisher,
            @Qualifier(TransactionTemplateConfig.REQUIRES_NEW) TransactionTemplate transactionTemplate,
            Clock clock) {
        this.roomAllocationRepository = roomAllocationRepository;
        this.applicationRepository = applicationRepository;
        this.reconfirmationPolicy = reconfirmationPolicy;
        this.roomOccupancyManager = roomOccupancyManager;
        this.allocationAuditService = allocationAuditService;
        this.waitingListService = waitingListService;
        this.allocationAccessGuard = allocationAccessGuard;
        this.roomAllocationMapper = roomAllocationMapper;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * The student confirms they are still coming. Their place keeps its bed. Checked by
     * {@link AllocationAccessGuard}: students are platform-tenant users, so the tenant filter does
     * nothing for them.
     */
    @Transactional
    public RoomAllocationResponse reconfirm(Long allocationId) {
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(allocationId)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + allocationId));
        allocationAccessGuard.assertCanAccessAllocation(allocation);

        if (allocation.getStatus() != AllocationStatus.ACCEPTED) {
            throw new InvalidAllocationStatusTransitionException(
                    "Only an accepted place can be reconfirmed; this one is " + allocation.getStatus());
        }
        if (allocation.getReconfirmedAt() == null) {
            allocation.setReconfirmedAt(LocalDateTime.now());
            allocation = roomAllocationRepository.save(allocation);
            log.info("action=placeReconfirmed allocationId={}", allocationId);
        }
        return roomAllocationMapper.toResponse(allocation);
    }

    /**
     * Daily job body, called by {@code ScheduledJobRunner}: asks, reminds and releases, one company
     * at a time.
     *
     * @return how many allocations were asked, reminded or released
     */
    public int runDaily() {
        LocalDate today = LocalDate.now(clock);
        Long previousTenant = TenantContext.getTenantId();
        int processed = 0;
        try {
            for (Long tenantId : roomAllocationRepository.findTenantIdsAwaitingReconfirmation(today)) {
                if (tenantId == null) {
                    continue;
                }
                TenantContext.setTenantId(tenantId);
                processed += runForCompany(tenantId, today);
            }
        } finally {
            if (previousTenant == null) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(previousTenant);
            }
        }
        return processed;
    }

    private int runForCompany(Long tenantId, LocalDate today) {
        ReconfirmationPolicy.LeadTimes leadTimes = reconfirmationPolicy.leadTimesFor(tenantId);
        List<Long> ids = roomAllocationRepository.findIdsAwaitingReconfirmation(tenantId, today);
        int processed = 0;
        for (Long id : ids) {
            try {
                Step step = transactionTemplate.execute(status -> handle(id, leadTimes, today));
                if (step != null && step != Step.NONE) {
                    processed++;
                }
            } catch (Exception e) {
                log.error("action=reconfirmationFailed allocationId={} tenantId={}", id, tenantId, e);
            }
        }
        log.info("action=reconfirmationRun tenantId={} candidates={} processed={}", tenantId, ids.size(), processed);
        return processed;
    }

    /** Decides and applies the step for one allocation, inside its own transaction. */
    Step handle(Long allocationId, ReconfirmationPolicy.LeadTimes leadTimes, LocalDate today) {
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(allocationId).orElse(null);
        if (allocation == null || allocation.getStatus() != AllocationStatus.ACCEPTED
                || allocation.getReconfirmedAt() != null || allocation.getStayStartDate() == null) {
            return Step.NONE;
        }
        LocalDate moveIn = allocation.getStayStartDate();
        if (!leadTimes.mustReconfirm(allocation.getAcceptedAt(), moveIn)) {
            return Step.NONE;
        }
        LocalDate releaseOn = leadTimes.releaseOn(moveIn);
        LocalDateTime requestedAt = allocation.getReconfirmRequestedAt();

        if (!today.isBefore(releaseOn)) {
            if (requestedAt == null) {
                // Never asked, for example because the settings changed: ask now rather than
                // release a place the student was never told about.
                return ask(allocation, false, today.plusDays(1));
            }
            if (requestedAt.toLocalDate().isBefore(today)) {
                release(allocation);
                return Step.RELEASED;
            }
            return Step.NONE;
        }
        if (today.isBefore(leadTimes.askOn(moveIn))) {
            return Step.NONE;
        }
        if (requestedAt == null) {
            return ask(allocation, false, releaseOn);
        }
        LocalDateTime lastContact = allocation.getReconfirmRemindedAt() != null
                ? allocation.getReconfirmRemindedAt() : requestedAt;
        if (!lastContact.toLocalDate().isAfter(today.minusDays(REMINDER_INTERVAL_DAYS))) {
            return ask(allocation, true, releaseOn);
        }
        return Step.NONE;
    }

    private Step ask(RoomAllocation allocation, boolean reminder, LocalDate releaseOn) {
        LocalDateTime now = LocalDateTime.now();
        if (reminder) {
            allocation.setReconfirmRemindedAt(now);
        } else {
            allocation.setReconfirmRequestedAt(now);
        }
        roomAllocationRepository.save(allocation);
        eventPublisher.publishEvent(new ReconfirmationRequestedEvent(allocation, reminder, releaseOn));
        log.info("action=reconfirmationRequested allocationId={} reminder={} releaseOn={}",
                allocation.getId(), reminder, releaseOn);
        return reminder ? Step.REMINDED : Step.ASKED;
    }

    /** ACCEPTED to CANCELLED, already a legal transition, and the bed goes back to the queue. */
    private void release(RoomAllocation allocation) {
        AllocationStatus previous = allocation.getStatus();
        AllocationStatusValidator.validate(allocation, AllocationStatus.CANCELLED);
        allocation.setStatus(AllocationStatus.CANCELLED);
        allocation.setEndDate(LocalDateTime.now());
        RoomAllocation saved = roomAllocationRepository.save(allocation);

        // After the status change, so the released bed is no longer counted (GH-136).
        roomOccupancyManager.syncOccupancy(saved.getRoom());
        allocationAuditService.logStatusChange(saved, previous, AllocationStatus.CANCELLED);
        cancelApplication(saved.getApplication());

        eventPublisher.publishEvent(new PlaceReleasedEvent(saved));
        eventPublisher.publishEvent(new RoomVacatedEvent(saved.getId(),
                saved.getRoom() != null ? saved.getRoom().getId() : null, AllocationStatus.CANCELLED));
        log.info("action=placeReleased reason=notReconfirmed allocationId={}", saved.getId());
    }

    private void cancelApplication(Application application) {
        if (application == null) {
            return;
        }
        waitingListService.closeEntriesForApplication(application.getId());
        if (!ApplicationStatusValidator.canApply(application, ApplicationStatus.CANCELLED)) {
            log.info("action=applicationCancelSkipped applicationId={} from={}",
                    application.getId(), application.getStatus());
            return;
        }
        application.setStatus(ApplicationStatus.CANCELLED);
        application.setRemarks("Place released: not reconfirmed before move-in.");
        applicationRepository.save(application);
    }
}
