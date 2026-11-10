package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

     List<Application> findByStatus(ApplicationStatus status);


     long countByStatus(ApplicationStatus status);

     List<Application> findByStudent_Id(Long studentId);

     /**
      * Applications for a student within one property company, newest first.
      *
      * <p>tenantId is an explicit parameter rather than left to the Hibernate tenant filter,
      * because tenant 0 is the sentinel that switches that filter off (GH-231).
      */
     List<Application> findByStudent_IdAndTenantIdAndStatusInOrderByIdDesc(
                     Long studentId, Long tenantId, Collection<ApplicationStatus> statuses);

     /** Applications for a student across every property company, newest first. */
     List<Application> findByStudent_IdAndStatusInOrderByIdDesc(
                     Long studentId, Collection<ApplicationStatus> statuses);

     /**
      * Everything {@code ApplicationMapper} dereferences, fetched in one query.
      *
      * <p>The mapper reads {@code student.appUser}, {@code preferredBuilding} and each building
      * preference's building, all LAZY. With {@code open-in-view} disabled those reads happened
      * after the repository transaction closed, so the detail endpoints threw
      * {@code LazyInitializationException} (GH-130). A read-only transaction on the service keeps
      * the session open; this graph also stops the mapper issuing a query per preference.
      */
     @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {
             "student", "student.appUser", "preferredBuilding",
             "buildingPreferences", "buildingPreferences.building",
             "academicYear", "intake", "tenant"})
     Optional<Application> findWithDetailsById(Long id);

     @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {
             "student", "student.appUser", "preferredBuilding",
             "buildingPreferences", "buildingPreferences.building",
             "academicYear", "intake", "tenant"})
     List<Application> findWithDetailsByStudent_Id(Long studentId);

     boolean existsByStudent_IdAndStatus(Long studentId, ApplicationStatus status);

     /** True when the student has an application in any of the given statuses; see GH-145. */
     boolean existsByStudent_IdAndStatusIn(Long studentId, Collection<ApplicationStatus> statuses);

     boolean existsByStudent_IdAndTenantIdAndStatus(Long studentId, Long tenantId, ApplicationStatus status);

     /** Whether a student has any application in the given tenant; drives tenant-admin document access. */
     boolean existsByStudent_IdAndTenantId(Long studentId, Long tenantId);

     /** Same check addressed by the student's login id, used when only the AppUser id is known. */
     boolean existsByStudent_AppUser_IdAndTenantId(Long appUserId, Long tenantId);

     boolean existsByStudent_IdAndTenantIdAndStatusIn(Long studentId, Long tenantId, java.util.Collection<ApplicationStatus> statuses);

     List<Application> findByStudent_IdAndStatus(Long studentId, ApplicationStatus status);

     void deleteByStudent(com.beezhub.student_accommodation.model.entity.Student student);

     boolean existsByAcademicYear_Id(Long academicYearId);

     long countByAcademicYear_Id(Long academicYearId);

     /**
      * Race-free application-code counter; see V2026_09_05_1100__application_code_sequence.sql.
      *
      * <p>nextval is a write, but Spring Data cannot infer that from a native SELECT, so this would
      * otherwise inherit SimpleJpaRepository's readOnly=true default and Postgres would reject it
      * outside a read-write transaction. MANDATORY declares the requirement, so a misuse fails at
      * the call site rather than as a confusing error from the driver.
      */
     @Transactional(propagation = Propagation.MANDATORY)
     @Query(value = "SELECT nextval('application_code_seq')", nativeQuery = true)
     long nextApplicationCodeSequence();

     @Query("SELECT COUNT(DISTINCT a.student.id) FROM Application a")
     long countDistinctStudents();
}
