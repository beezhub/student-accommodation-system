package com.beezhub.student_accommodation.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

     List<Application> findByStatus(ApplicationStatus status);
     long countByApplicationCodeStartingWith(String s);
     List<Application> findByStudent_Id(Long studentId);
    Optional<Application> findTopByStudent_IdOrderByApplicationDateDesc(Long studentId);
}
