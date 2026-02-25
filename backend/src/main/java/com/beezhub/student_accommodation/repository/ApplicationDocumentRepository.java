package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.ApplicationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository for the {@link ApplicationDocument} junction table.
 *
 * <p>Introduced in TASK-2.01 to support student-owned documents that can be
 * attached to multiple applications.</p>
 */
@Repository
public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {

    /**
     * Finds all junction entries for a given application.
     * Used in checklist and review workflows.
     */
    List<ApplicationDocument> findByApplication_Id(Long applicationId);

    /**
     * Finds all junction entries for a given document.
     * Used to determine which applications a document is attached to.
     */
    List<ApplicationDocument> findByDocument_Id(Long documentId);

    /**
     * Batch query: finds all junction entries for a list of application IDs.
     * Used for efficient enrichment in search results.
     */
    List<ApplicationDocument> findByApplication_IdIn(List<Long> applicationIds);

    /**
     * Checks if a document is already attached to a given application.
     */
    boolean existsByApplication_IdAndDocument_Id(Long applicationId, Long documentId);

    /**
     * Removes the association between a document and an application.
     */
    @Transactional
    void deleteByApplication_IdAndDocument_Id(Long applicationId, Long documentId);
}
