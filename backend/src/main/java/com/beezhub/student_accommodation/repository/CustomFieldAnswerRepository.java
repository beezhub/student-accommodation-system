package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.CustomFieldAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomFieldAnswerRepository extends JpaRepository<CustomFieldAnswer, Long> {

    List<CustomFieldAnswer> findByApplication_Id(Long applicationId);

    List<CustomFieldAnswer> findByField_Id(Long fieldId);
}
