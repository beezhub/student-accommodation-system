package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    List<Document> findAllByStudent_AppUser_Id(Long userId);
    long countByDocumentStatus(DocumentStatus status);
}
