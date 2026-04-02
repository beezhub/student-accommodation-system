package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
