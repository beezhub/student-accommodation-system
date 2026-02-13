package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

     List<Application> findByStatus(ApplicationStatus status);

     long countByApplicationCodeStartingWith(String s);

     long countByStatus(ApplicationStatus status);

     List<Application> findByStudent_Id(Long studentId);

     boolean existsByStudent_IdAndStatus(Long studentId, ApplicationStatus status);

     List<Application> findByStudent_IdAndStatus(Long studentId, ApplicationStatus status);

     void deleteByStudent(com.beezhub.student_accommodation.model.entity.Student student);

     boolean existsByAcademicYear_Id(Long academicYearId);

     @Query("SELECT MAX(a.applicationCode) FROM Application a WHERE a.applicationCode LIKE :prefix%")
     Optional<String> findMaxApplicationCodeByPrefix(@Param("prefix") String prefix);
}
