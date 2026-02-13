package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    // ==================================================
    // EXISTING METHODS (no fetch)
    // ==================================================

    Optional<Student>findStudentByAppUser(AppUser User);

    Optional<Student> findByAppUser_Id(Long userId);

    // ==================================================
    // FULL FETCH (all associations)
    // Use case: Student profile, admin user management
    // ==================================================

    /**
     * Finds a student by ID with all associations eagerly loaded.
     * @param id Student ID
     * @return Student with appUser, institution, and yearOfStudy loaded
     */
    @EntityGraph(attributePaths = {"appUser", "institution", "yearOfStudy"})
    @Query("SELECT s FROM Student s WHERE s.id = :id")
    Optional<Student> findStudentWithDetailsById(@Param("id") Long id);

    /**
     * Finds a student by user ID with all associations eagerly loaded.
     * @param userId AppUser ID
     * @return Student with all details
     */
    @EntityGraph(attributePaths = {"appUser", "institution", "yearOfStudy"})
    @Query("SELECT s FROM Student s WHERE s.appUser.id = :userId")
    Optional<Student> findStudentWithDetailsByUserId(@Param("userId") Long userId);

    /**
     * Finds all students with full details.
     * @return List of students with all associations loaded
     */
    @EntityGraph(attributePaths = {"appUser", "institution", "yearOfStudy"})
    List<Student> findAllStudentsWithDetails();

    // ==================================================
    // PARTIAL FETCH (appUser only)
    // Use case: Notifications (need email only)
    // ==================================================

    /**
     * Finds a student by ID with appUser association loaded.
     * @param id Student ID
     * @return Student with appUser loaded
     */
    @EntityGraph(attributePaths = {"appUser"})
    @Query("SELECT s FROM Student s WHERE s.id = :id")
    Optional<Student> findStudentWithUserById(@Param("id") Long id);

    /**
     * Finds a student by AppUser with appUser association loaded.
     * @param user AppUser entity
     * @return Student with appUser loaded
     */
    @EntityGraph(attributePaths = {"appUser"})
    @Query("SELECT s FROM Student s WHERE s.appUser = :user")
    Optional<Student> findStudentWithUserByAppUser(@Param("user") AppUser user);

    // ==================================================
    // PARTIAL FETCH (academic details)
    // Use case: Allocation scoring (needs institution, yearOfStudy)
    // ==================================================

    /**
     * Finds a student by ID with academic details loaded.
     * @param id Student ID
     * @return Student with institution and yearOfStudy loaded
     */
    @EntityGraph(attributePaths = {"institution", "yearOfStudy"})
    @Query("SELECT s FROM Student s WHERE s.id = :id")
    Optional<Student> findStudentWithAcademicDetailsById(@Param("id") Long id);
}
