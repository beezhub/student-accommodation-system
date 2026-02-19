package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    List<Document> findAllByStudent_AppUser_Id(Long userId);

    List<Document> findAllByStudent_Id(Long studentId);

    long countByDocumentStatus(DocumentStatus status);

    void deleteByStudent(com.beezhub.student_accommodation.model.entity.Student student);

    // Queries by application
    List<Document> findByApplicationId(Long applicationId);

    List<Document> findByApplicationIdAndDocumentType_Id(Long applicationId, Long documentTypeId);

    Optional<Document> findTopByApplicationIdAndDocumentType_IdOrderByCreatedAtDesc(Long applicationId,
            Long documentTypeId);

    // Batch query
    List<Document> findByApplicationIdIn(List<Long> applicationIds);

    boolean existsByDocumentPath(String documentPath);
}
