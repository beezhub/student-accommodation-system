package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    List<Document> findAllByStudent_AppUser_Id(Long userId);

    List<Document> findAllByStudent_Id(Long studentId);

    /** Batch fetch — used to enrich application search results without N+1 queries. */
    List<Document> findAllByStudent_IdIn(List<Long> studentIds);

    /** Upsert lookup: find the document for this student + type slot (at most one due to unique constraint). */
    @Query("SELECT d FROM Document d WHERE d.student.id = :studentId AND d.documentType.id = :documentTypeId")
    Optional<Document> findByStudentIdAndDocumentTypeId(@Param("studentId") Long studentId,
                                                         @Param("documentTypeId") Long documentTypeId);

    /** Returns the set of document_type_id values already uploaded by the student. */
    @Query("SELECT d.documentType.id FROM Document d WHERE d.student.id = :studentId")
    Set<Long> findDocumentTypeIdsByStudentId(@Param("studentId") Long studentId);

    /** Returns the set of document type names already uploaded by the student — used for name-based completeness checks. */
    @Query("SELECT d.documentType.typeName FROM Document d WHERE d.student.id = :studentId")
    Set<String> findDocumentTypeNamesByStudentId(@Param("studentId") Long studentId);

    long countByDocumentStatus(DocumentStatus status);

    void deleteByStudent(com.beezhub.student_accommodation.model.entity.Student student);

    boolean existsByDocumentPath(String documentPath);

    /**
     * Documents past their retention period: nothing the student has is still in use, and
     * everything they had went quiet longer ago than {@code cutoff} (GH-235).
     *
     * <p>The retention clock is the application's, not the document's. A pack is reused across
     * applications to different companies, so expiring on upload date would delete evidence a
     * student is still relying on somewhere else. {@code Document} carries no link to an
     * application, so the anchor is derived from the student's own history.
     *
     * <p>Two things keep a pack alive, and both are needed:
     *
     * <ul>
     *   <li><b>A live application</b> — {@code inUseStatuses}, which the caller passes as
     *       {@code LIVE_STATUSES} <em>minus</em> {@code CHECKED_IN}. That exclusion is the point:
     *       {@code CHECKED_IN} is terminal in the application state machine, so an application
     *       never leaves it and a pack keyed on it alone would be kept for ever, exempting exactly
     *       the students the rule is about.</li>
     *   <li><b>An allocation holding a bed</b> — {@code heldStatuses}: OFFERED, ACCEPTED, ACTIVE.
     *       This is what actually tracks residency. A resident's allocation is ACTIVE while they
     *       live there and COMPLETED or TERMINATED once they move out, so it, not the application
     *       status, is what says a tenancy has ended.</li>
     * </ul>
     *
     * <p>Both {@code COALESCE} terms fall back to the document's own creation date, so the whole
     * condition reads as "the latest thing that happened to this student was before the cutoff",
     * and for a student who uploaded documents and never applied — the one case with no clock to
     * read at all — it reduces to the upload date.
     *
     * <p>No tenant scoping, deliberately. The exclusions must see every company's rows: a pack
     * supporting a live application at company B has to survive even though company A's
     * application closed a year ago. {@code Document} is not tenant-scoped ({@code CLAUDE.md}
     * records why) and {@code Application} is only filtered when a tenant is in context, which
     * never happens on a scheduler thread.
     *
     * <p>Ordered and paged because the caller deletes as it goes; the page is re-read each round
     * rather than offset, since deleted rows have left the result set.
     */
    @Query("""
            SELECT d FROM Document d
            JOIN FETCH d.student s
            WHERE NOT EXISTS (
                    SELECT 1 FROM Application live
                    WHERE live.student.id = s.id AND live.status IN :inUseStatuses)
              AND NOT EXISTS (
                    SELECT 1 FROM RoomAllocation held
                    WHERE held.student.id = s.id AND held.status IN :heldStatuses)
              AND COALESCE(
                    (SELECT MAX(a.updatedAt) FROM Application a WHERE a.student.id = s.id),
                    d.createdAt) < :cutoff
              AND COALESCE(
                    (SELECT MAX(r.updatedAt) FROM RoomAllocation r WHERE r.student.id = s.id),
                    d.createdAt) < :cutoff
            ORDER BY d.id
            """)
    List<Document> findExpiredByRetention(
            @Param("inUseStatuses") Collection<ApplicationStatus> inUseStatuses,
            @Param("heldStatuses") Collection<AllocationStatus> heldStatuses,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable);
}
