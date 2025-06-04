package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByStudent_AppUser_Id(Long userId);
}
