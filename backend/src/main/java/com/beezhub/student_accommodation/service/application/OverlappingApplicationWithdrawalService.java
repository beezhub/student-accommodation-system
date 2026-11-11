package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.config.TransactionTemplateConfig;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.event.ApplicationsWithdrawnEvent;
import com.beezhub.student_accommodation.model.event.ApplicationsWithdrawnEvent.Withdrawn;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.service.RoomAllocationService;
import com.beezhub.student_accommodation.service.allocation.ApplicationStatusValidator;
import com.beezhub.student_accommodation.service.allocation.StayPeriod;
import com.beezhub.student_accommodation.service.allocation.WaitingListService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Withdraws a student's other applications once they accept a place (GH-497): every active
 * application, at any company, whose intake stay overlaps the accepted stay is cancelled, with any
 * open offer on it, so the student no longer holds places at other companies for the same dates.
 *
 * <p>This is the first write across companies. It runs after the accept has committed, outside the
 * request's tenant filter on purpose, and selects by the student's own id only, so it can never
 * reach another student's applications. Each application is then handled in its own transaction
 * with the tenant context set to its company, so audit rows and the waiting-list refill stay with
 * that company, and one failure costs one application.
 *
 * <p>Freed beds go through {@code RoomVacatedEvent} (published by the offer release), and
 * {@link ApplicationsWithdrawnEvent} carries what was withdrawn to the student's email and each
 * company's notification.
 */
@Service
@Slf4j
public class OverlappingApplicationWithdrawalService {

    /**
     * Applications that are still unfinished. ALLOCATED is left out on purpose: the student already
     * holds an accepted place there, and giving it up is their decision, not a side effect.
     */
    static final Set<ApplicationStatus> WITHDRAWABLE = EnumSet.of(
            ApplicationStatus.PENDING,
            ApplicationStatus.UNDER_REVIEW,
            ApplicationStatus.CHANGES_REQUESTED,
            ApplicationStatus.APPROVED,
            ApplicationStatus.AWAITING_ALLOCATION);

    /** Places the student already holds; an application with one is not withdrawn. */
    private static final Set<AllocationStatus> HELD = EnumSet.of(AllocationStatus.ACCEPTED, AllocationStatus.ACTIVE);

    static final String REMARK = "Withdrawn: the student accepted a place for overlapping dates.";

    private final ApplicationRepository applicationRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final RoomAllocationService roomAllocationService;
    private final WaitingListService waitingListService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public OverlappingApplicationWithdrawalService(
            ApplicationRepository applicationRepository,
            RoomAllocationRepository roomAllocationRepository,
            RoomAllocationService roomAllocationService,
            WaitingListService waitingListService,
            ApplicationEventPublisher eventPublisher,
            @Qualifier(TransactionTemplateConfig.REQUIRES_NEW) TransactionTemplate transactionTemplate) {
        this.applicationRepository = applicationRepository;
        this.roomAllocationRepository = roomAllocationRepository;
        this.roomAllocationService = roomAllocationService;
        this.waitingListService = waitingListService;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    /** The accepted place and the applications it overlaps. */
    record Accepted(Long studentId, List<Candidate> candidates) {
    }

    /** An application to withdraw, and its company. */
    record Candidate(Long applicationId, Long tenantId) {
    }

    /**
     * Withdraws the overlapping applications of the student who accepted {@code acceptedAllocationId}.
     * Restores the caller's tenant context afterwards.
     *
     * @return how many applications were withdrawn
     */
    public int withdrawOverlapping(Long acceptedAllocationId) {
        Long previousTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
            Accepted accepted = transactionTemplate.execute(status -> findOverlapping(acceptedAllocationId));
            if (accepted == null || accepted.candidates().isEmpty()) {
                return 0;
            }
            List<Withdrawn> withdrawn = new ArrayList<>();
            for (Candidate candidate : accepted.candidates()) {
                TenantContext.setTenantId(candidate.tenantId());
                try {
                    Withdrawn done = transactionTemplate.execute(
                            status -> withdraw(candidate.applicationId(), accepted.studentId()));
                    if (done != null) {
                        withdrawn.add(done);
                    }
                } catch (Exception e) {
                    log.error("action=applicationWithdrawalFailed applicationId={} tenantId={} acceptedAllocationId={}",
                            candidate.applicationId(), candidate.tenantId(), acceptedAllocationId, e);
                }
            }
            if (!withdrawn.isEmpty()) {
                TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
                ApplicationsWithdrawnEvent event =
                        new ApplicationsWithdrawnEvent(accepted.studentId(), acceptedAllocationId, List.copyOf(withdrawn));
                transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));
            }
            log.info("action=overlappingApplicationsWithdrawn acceptedAllocationId={} candidates={} withdrawn={}",
                    acceptedAllocationId, accepted.candidates().size(), withdrawn.size());
            return withdrawn.size();
        } finally {
            if (previousTenant == null) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(previousTenant);
            }
        }
    }

    /**
     * The accepted place, and the student's other withdrawable applications whose stay overlaps it.
     * Selected by the student's id alone, across every company.
     */
    Accepted findOverlapping(Long acceptedAllocationId) {
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(acceptedAllocationId).orElse(null);
        if (allocation == null || allocation.getStatus() != AllocationStatus.ACCEPTED || allocation.getStudent() == null) {
            return null;
        }
        StayPeriod stay = StayPeriod.of(allocation);
        if (isUnbounded(stay)) {
            // No planned dates means nothing says which applications it overlaps: withdraw none.
            log.info("action=applicationWithdrawalSkipped reason=noStayDates acceptedAllocationId={}", acceptedAllocationId);
            return null;
        }
        Long studentId = allocation.getStudent().getId();
        Long acceptedApplicationId = allocation.getApplication() != null ? allocation.getApplication().getId() : null;
        List<Candidate> candidates = applicationRepository.findByStudent_IdAndStatusInOrderByIdDesc(studentId, WITHDRAWABLE)
                .stream()
                .filter(application -> !Objects.equals(application.getId(), acceptedApplicationId))
                .filter(application -> {
                    StayPeriod other = StayPeriod.of(application);
                    return !isUnbounded(other) && other.overlaps(stay);
                })
                .map(application -> new Candidate(application.getId(), application.getTenantId()))
                .toList();
        return new Accepted(studentId, candidates);
    }

    /** Cancels one application and its open offers, inside its own transaction. Re-checks the owner. */
    Withdrawn withdraw(Long applicationId, Long studentId) {
        Application application = applicationRepository.findWithDetailsById(applicationId).orElse(null);
        if (application == null || application.getStudent() == null
                || !Objects.equals(application.getStudent().getId(), studentId)
                || !WITHDRAWABLE.contains(application.getStatus())) {
            return null;
        }
        if (!roomAllocationRepository.findByApplicationIdAndStatusIn(applicationId, HELD).isEmpty()) {
            log.info("action=applicationWithdrawalSkipped reason=placeHeld applicationId={}", applicationId);
            return null;
        }

        ApplicationStatus previous = application.getStatus();
        ApplicationStatusValidator.validate(application, ApplicationStatus.CANCELLED);
        // OFFERED to CANCELLED for any open offer; each freed bed publishes RoomVacatedEvent.
        int offersCancelled = roomAllocationService.releaseOutstandingAllocations(applicationId, "withdrawnOnOtherAcceptance");
        waitingListService.closeEntriesForApplication(applicationId);
        application.setStatus(ApplicationStatus.CANCELLED);
        application.setRemarks(REMARK);
        applicationRepository.save(application);

        log.info("action=applicationWithdrawn applicationId={} tenantId={} from={} offersCancelled={}",
                applicationId, application.getTenantId(), previous, offersCancelled);
        return new Withdrawn(applicationId, application.getApplicationCode(), application.getTenantId(),
                application.getTenant() != null ? application.getTenant().getName() : null,
                intakeLabel(application), offersCancelled);
    }

    private static boolean isUnbounded(StayPeriod stay) {
        return stay.start() == null && stay.end() == null;
    }

    private static String intakeLabel(Application application) {
        if (application.getIntake() == null || application.getIntake().getAcademicYear() == null) {
            return null;
        }
        return CompanyIntakePolicy.label(application.getIntake());
    }
}
