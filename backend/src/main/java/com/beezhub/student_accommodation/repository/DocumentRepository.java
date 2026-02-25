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

    List<Document> findAllByStudent_Id(Long studentId);

    long countByDocumentStatus(DocumentStatus status);

    void deleteByStudent(com.beezhub.student_accommodation.model.entity.Student student);

    boolean existsByDocumentPath(String documentPath);
}
