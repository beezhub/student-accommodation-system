package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.AllocationRule;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.Student;

import java.util.List;

/**
 * Strategy interface for room scoring and hard-constraint validation.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@code LegacyScoringStrategy} — v1.1 hardcoded weights (default/fallback)</li>
 *   <li>{@code OwnerConfigurableScoringStrategy} — v2.0 owner-configured weights (TASK-2.11)</li>
 * </ul>
 */
public interface ScoringStrategy {

    /**
     * Scores all available rooms for a student and application.
     * Returns list sorted by score DESC, then by application createdAt ASC (tie-breaking).
     *
     * @param student     The student to score rooms for
     * @param application The application containing preferences
     * @param preferred   Optional preferred building override
     * @return List of scored rooms sorted by score descending
     */
    List<ScoredRoom> scoreAvailableRooms(Student student, Application application, Building preferred);

    /**
     * Calculates the score for a single student-room pair.
     *
     * @param student     The student
     * @param application The application with preferences
     * @param room        The room to score
     * @param rule        The allocation rule containing scoring weights
     * @return ScoredRoom with calculated score and warnings
     */
    ScoredRoom scoreSingleRoom(Student student, Application application, Room room, AllocationRule rule);

    /**
     * Validates hard constraints (gender, accessibility, year of study, institution, prior declines).
     *
     * @param student The student
     * @param room    The room
     * @return true if all hard constraints pass
     */
    boolean satisfiesHardConstraints(Student student, Room room);
}
