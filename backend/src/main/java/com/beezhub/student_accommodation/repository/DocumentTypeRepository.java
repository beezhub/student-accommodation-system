package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

    List<DocumentType> findByIsRequiredTrue();

}
