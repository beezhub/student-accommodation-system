package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.exceptions.DocumentsNotReadyException;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.CompanyRules;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.DocumentIssue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds room offers and check-in back while a student's documents are not in order (GH-498): a
 * required expiring document that is expired or not yet approved. Applying is never blocked, and a
 * student skipped by the batch keeps their place in the queue.
 *
 * <p>Read against the company that owns the bed and the institution the application was made for,
 * since expiry is worked out per company. Documents are deliberately not tenant-scoped, so the pack
 * is read by student id.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentReadinessGuard {

    private final DocumentExpiryPolicy documentExpiryPolicy;
    private final DocumentRepository documentRepository;

    /** What stops a room offer at {@code tenantId} for this student and application. */
    public List<DocumentIssue> issuesForRoom(Student student, Long tenantId, Application application) {
        return issuesForRoom(student, documentExpiryPolicy.forCompany(tenantId), application);
    }

    /** As above, with the company's rules already loaded, for the batch. */
    public List<DocumentIssue> issuesForRoom(Student student, CompanyRules rules, Application application) {
        if (student == null || student.getId() == null || !rules.hasExpiringTypes()) {
            return List.of();
        }
        return rules.issuesForRoom(documentRepository.findAllByStudent_Id(student.getId()),
                DocumentExpiryPolicy.institutionOf(application, student));
    }

    /** Throws unless the student's documents allow a room offer at {@code tenantId}. */
    public void assertReadyForRoom(Student student, Long tenantId, Application application) {
        List<DocumentIssue> issues = issuesForRoom(student, tenantId, application);
        if (!issues.isEmpty()) {
            log.warn("action=offerRefused reason=documentsNotReady studentId={} tenantId={} issues={}",
                    student.getId(), tenantId, issues.size());
            throw new DocumentsNotReadyException("A room cannot be offered until these documents are in order: "
                    + describe(issues) + ".");
        }
    }

    /** Throws unless the student's documents allow them to check in to the allocated room. */
    public void assertReadyForCheckIn(RoomAllocation allocation) {
        Room room = allocation.getRoom();
        Long tenantId = room != null && room.getTenantId() != null ? room.getTenantId() : allocation.getTenantId();
        List<DocumentIssue> issues = issuesForRoom(allocation.getStudent(), tenantId, allocation.getApplication());
        if (!issues.isEmpty()) {
            log.warn("action=checkInRejected reason=documentsNotReady allocationId={} issues={}",
                    allocation.getId(), issues.size());
            throw new DocumentsNotReadyException("The student cannot check in until these documents are in order: "
                    + describe(issues) + ". A replacement needs an administrator's approval.");
        }
    }

    /**
     * What stops a stay extension: every required document approved, and none of the expiring ones
     * expired.
     */
    public List<DocumentIssue> issuesForStay(RoomAllocation allocation) {
        Student student = allocation.getStudent();
        if (student == null || student.getId() == null) {
            return List.of();
        }
        CompanyRules rules = documentExpiryPolicy.forCompany(allocation.getTenantId());
        return rules.issuesForStay(documentRepository.findAllByStudent_Id(student.getId()),
                DocumentExpiryPolicy.institutionOf(allocation.getApplication(), student));
    }

    public static String describe(List<DocumentIssue> issues) {
        return issues.stream().map(DocumentIssue::describe).collect(Collectors.joining(", "));
    }
}
