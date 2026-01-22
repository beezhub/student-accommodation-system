package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.DocumentComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentCommentRepository extends JpaRepository<DocumentComment, Long> {

    List<DocumentComment> findByDocument_Id(Long documentId);

    List<DocumentComment> findByAdmin_Id(Long adminId);

    List<DocumentComment> findByDocument_IdOrderByCreatedAtDesc(Long documentId);
}
