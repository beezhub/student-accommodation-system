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

    /**
     * Fetches everything room scoring reads. The batch job calls this with no transaction and scores
     * in a later session, so anything left lazy here fails with LazyInitializationException. The
     * intake and year give the planned stay the batch matches beds for (GH-496).
     */
    @Query("SELECT w FROM WaitingListEntry w " +
           "LEFT JOIN FETCH w.student s " +
           "LEFT JOIN FETCH s.appUser " +
           "LEFT JOIN FETCH s.yearOfStudy " +
           "LEFT JOIN FETCH w.application a " +
           "LEFT JOIN FETCH a.preferredBuilding " +
           "LEFT JOIN FETCH a.buildingPreferences " +
           "LEFT JOIN FETCH a.intake " +
           "LEFT JOIN FETCH a.academicYear " +
           "WHERE w.status = :status " +
           "ORDER BY w.createdAt ASC")
    List<WaitingListEntry> findByStatusOrderByCreatedAtAsc(@Param("status") String status);

    List<WaitingListEntry> findAllByOrderByCreatedAtAsc();

    Optional<WaitingListEntry> findByStudent_Id(Long studentId);

    Optional<WaitingListEntry> findByStudent_IdAndStatus(Long studentId, String status);

    List<WaitingListEntry> findByApplication_IdAndStatus(Long applicationId, String status);

    long countByStatus(String status);

    /**
     * Students waiting in an academic year whose stay has not ended (queue depth). An entry for an
     * intake that is over is never offered a room, so it is not counted (GH-496).
     */
    @Query("SELECT COUNT(w) FROM WaitingListEntry w JOIN w.application a LEFT JOIN a.intake i " +
           "WHERE w.status = 'WAITING' AND a.academicYear.id = :academicYearId " +
           "AND (i.id IS NULL OR i.stayEndDate > :today)")
    long countWaitingInYear(@Param("academicYearId") Long academicYearId,
                            @Param("today") java.time.LocalDate today);

    /**
     * As {@link #countWaitingInYear}, for entries created before {@code createdAt}: the student's
     * 1-based queue position is this count plus one.
     */
    @Query("SELECT COUNT(w) FROM WaitingListEntry w JOIN w.application a LEFT JOIN a.intake i " +
           "WHERE w.status = 'WAITING' AND a.academicYear.id = :academicYearId " +
           "AND w.createdAt < :createdAt AND (i.id IS NULL OR i.stayEndDate > :today)")
    long countWaitingInYearBefore(@Param("academicYearId") Long academicYearId,
                                  @Param("createdAt") java.time.LocalDateTime createdAt,
                                  @Param("today") java.time.LocalDate today);
}
