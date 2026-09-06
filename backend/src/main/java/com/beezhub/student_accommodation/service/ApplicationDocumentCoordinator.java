package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.RoomAllocationRequest;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.beezhub.student_accommodation.model.enums.DocumentStatus.APPROVED;

import com.beezhub.student_accommodation.exceptions.ApplicationNotApprovedException;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationDocumentCoordinator {

    private final DocumentRepository documentRepository;
    private final RoomAllocationService roomAllocationService;
    private final RoomAllocationRepository roomAllocationRepository;
    private final ApplicationRepository applicationRepository;
    private final DocumentTypeService documentTypeService;

    /**
     * The application a document review is acting on: the student's most recent live application,
     * within the reviewing admin's company where they have one.
     *
     * <p>Was {@code findApprovedApplication}, which required status exactly {@code APPROVED} and
     * ignored the tenant entirely. Both were wrong:
     *
     * <ul>
     *   <li>Approval moves an application straight to {@code AWAITING_ALLOCATION}, so the window in
     *       which review succeeded closed the moment the application was approved. That left an
     *       admin no way to reject a wrongly approved document except deleting it, which loses the
     *       record of what was approved — and {@code saveReviewProgress}, the other half of the same
     *       screen, allowed review at any status, so the two guards disagreed (GH-217, GH-231).</li>
     *   <li>It returned the first approved application it found, in no defined order and with no
     *       tenant scope. A platform admin carries tenant 0, the sentinel that disables the
     *       Hibernate tenant filter, so the lookup could return an application belonging to a
     *       different company from the one reviewing the document — and that application is what
     *       {@code tryAllocateOrQueue} then allocates a room against (GH-231).</li>
     * </ul>
     *
     * <p>The accepted statuses are {@link ApplicationStatus#LIVE_STATUSES}, the same set that
     * decides whether an approved document is locked, so review is available in exactly the states
     * the lock covers.
     *
     * <p>The scope comes from {@code DocumentAccessGuard.tenantScopeOf}, which is how every other
     * document rule decides what an admin may reach. The document's own {@code tenant_id} is
     * deliberately not used: documents are student-owned and carry no tenant filter, so that column
     * holds whatever context the student uploaded under rather than the owning company.
     *
     * @param tenantScope the reviewing admin's company, or empty for a platform admin
     * @throws IllegalStateException when the caller sees every tenant and the student has live
     *     applications with more than one company, so there is no single right answer. Guessing
     *     would allocate a room at the wrong company.
     */
    public Application findReviewableApplication(Long studentId, Optional<Long> tenantScope) {
        List<Application> live = tenantScope
                .map(tenantId -> applicationRepository.findByStudent_IdAndTenantIdAndStatusInOrderByIdDesc(
                        studentId, tenantId, ApplicationStatus.LIVE_STATUSES))
                .orElseGet(() -> applicationRepository.findByStudent_IdAndStatusInOrderByIdDesc(
                        studentId, ApplicationStatus.LIVE_STATUSES));

        if (live.isEmpty()) {
            throw new ApplicationNotApprovedException(
                    "Documents can only be reviewed while the student has an active application");
        }

        long distinctTenants = live.stream().map(Application::getTenantId).distinct().count();
        if (distinctTenants > 1) {
            log.warn("action=ambiguousDocumentReview studentId={} tenants={}", studentId, distinctTenants);
            throw new IllegalStateException(
                    "This student has active applications with more than one property company, so "
                            + "there is no single application to review this document against. "
                            + "Review it from that company's admin account.");
        }

        return live.get(0);
    }

    @Transactional
    public void tryAllocateOrQueue(Student student, Application application) {
        // Fetch documents from the student's document pack (documents live at student level)
        List<Document> documents = documentRepository.findAllByStudent_Id(student.getId());

        // Use the application's tenant effective required type names (name-based, not platform flag).
        // This ensures tenant shadows of the isRequired flag are respected at the allocation gate.
        java.util.Set<String> requiredTypeNames = documentTypeService
                .getEffectiveRequiredTypeNames(application.getTenantId());

        long requiredApproved = documents.stream()
                .filter(doc -> doc.getDocumentStatus() == APPROVED)
                .filter(doc -> requiredTypeNames.contains(doc.getDocumentType().getTypeName().toLowerCase()))
                .count();

        if (requiredApproved < requiredTypeNames.size()) {
            log.info("Student {} does not have all required documents approved yet ({}/{} approved)",
                    student.getId(), requiredApproved, requiredTypeNames.size());
            return;
        }

        try {
            Room availableRoom = roomAllocationService.findAvailableRoom();
            if (availableRoom != null) {
                RoomAllocationRequest request = new RoomAllocationRequest();
                request.setStudentId(student.getId());
                request.setRoomId(availableRoom.getId());
                request.setApplicationId(application.getId());
                roomAllocationService.createRoomAllocation(request);
                log.info("Allocated room {} to student {} after document approvals", availableRoom.getRoomNumber(),
                        student.getId());
            } else {
                roomAllocationService.enqueueWaitingList(student.getId(), application.getId());
            }
        } catch (com.beezhub.student_accommodation.exceptions.RoomAllocationNotFoundException |
                 com.beezhub.student_accommodation.exceptions.RoomCapacityExceededException |
                 com.beezhub.student_accommodation.exceptions.StudentAlreadyAllocatedException e) {
            log.error("Failed to auto-allocate room for student {}", student.getId(), e);
        }
    }

    @Transactional(readOnly = true)
    public RoomAllocation findActiveAllocation(Long studentId) {
        return roomAllocationRepository.findActiveAllocationByStudentId(studentId).orElse(null);
    }
}
