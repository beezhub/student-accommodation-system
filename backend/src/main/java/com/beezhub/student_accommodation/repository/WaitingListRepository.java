package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitingListRepository extends JpaRepository<WaitingListEntry, Long> {
    Optional<WaitingListEntry> findFirstByStatusOrderByCreatedAtAsc(String status);
    boolean existsByStudent_IdAndStatus(Long studentId, String status);
    void deleteByStudent(com.beezhub.student_accommodation.model.entity.Student student);

    @Query("SELECT w FROM WaitingListEntry w " +
           "LEFT JOIN FETCH w.student s " +
           "LEFT JOIN FETCH s.appUser " +
           "LEFT JOIN FETCH w.application a " +
           "LEFT JOIN FETCH a.preferredBuilding " +
           "WHERE w.status = :status " +
           "ORDER BY w.createdAt ASC")
    List<WaitingListEntry> findByStatusOrderByCreatedAtAsc(@Param("status") String status);

    List<WaitingListEntry> findAllByOrderByCreatedAtAsc();

    Optional<WaitingListEntry> findByStudent_Id(Long studentId);

    Optional<WaitingListEntry> findByStudent_IdAndStatus(Long studentId, String status);

    long countByStatus(String status);

    /** Total students waiting, scoped to a specific academic year (queue depth). */
    long countByStatusAndApplication_AcademicYear_Id(String status, Long academicYearId);

    /**
     * Number of waiting-list entries ahead of the given timestamp within an
     * academic year — used to compute a student's 1-based queue position
     * (position = count + 1).
     */
    long countByStatusAndApplication_AcademicYear_IdAndCreatedAtLessThan(
            String status, Long academicYearId, java.time.LocalDateTime createdAt);
}

