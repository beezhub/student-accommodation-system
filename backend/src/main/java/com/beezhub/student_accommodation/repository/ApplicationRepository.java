package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

     List<Application> findByStatus(ApplicationStatus status);
     long countByApplicationCodeStartingWith(String s);
     List<Application> findByStudent_Id(Long studentId);
}
