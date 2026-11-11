package com.beezhub.student_accommodation.specification;

import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RoomAllocationSpecification {

    public static Specification<RoomAllocation> filterBy(Long studentId, Long roomId, Long buildingId,
            LocalDateTime fromDate, LocalDateTime toDate) {
        return filterBy(studentId, roomId, buildingId, fromDate, toDate, null, null, null);
    }

    public static Specification<RoomAllocation> filterBy(Long studentId, Long roomId, Long buildingId,
            LocalDateTime fromDate, LocalDateTime toDate,
            String studentName, String roomNumber, String status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (studentId != null) {
                predicates.add(criteriaBuilder.equal(root.get("student").get("id"), studentId));
            }

            if (roomId != null) {
                predicates.add(criteriaBuilder.equal(root.get("room").get("id"), roomId));
            }

            if (buildingId != null) {
                predicates.add(criteriaBuilder.equal(root.get("room").get("building").get("id"), buildingId));
            }

            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("allocationDate"), fromDate));
            }

            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("allocationDate"), toDate));
            }

            // Filter by student name (searches first name or last name, case-insensitive)
            if (studentName != null && !studentName.trim().isEmpty()) {
                String searchPattern = "%" + studentName.trim().toLowerCase() + "%";
                Predicate firstNameMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("student").get("appUser").get("firstName")),
                        searchPattern);
                Predicate lastNameMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("student").get("appUser").get("lastName")),
                        searchPattern);
                predicates.add(criteriaBuilder.or(firstNameMatch, lastNameMatch));
            }

            // Filter by room number (partial match, case-insensitive)
            if (roomNumber != null && !roomNumber.trim().isEmpty()) {
                String searchPattern = "%" + roomNumber.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("room").get("roomNumber")),
                        searchPattern));
            }

            // Filter by status
            if (status != null && !status.trim().isEmpty()) {
                try {
                    com.beezhub.student_accommodation.model.enums.AllocationStatus statusEnum = com.beezhub.student_accommodation.model.enums.AllocationStatus
                            .fromString(status.trim());
                    predicates.add(criteriaBuilder.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException e) {
                    // If invalid status, return empty result or ignore?
                    // Better to return empty result for strict filtering
                    predicates.add(criteriaBuilder.disjunction());
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Allocations whose application is for one institution and/or one intake (GH-496). Either may
     * be null to leave that side unfiltered. An allocation with no application matches neither.
     */
    public static Specification<RoomAllocation> forInstitutionAndIntake(Long institutionId, Long intakeId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (institutionId != null || intakeId != null) {
                var application = root.join("application", jakarta.persistence.criteria.JoinType.INNER);
                if (institutionId != null) {
                    predicates.add(cb.equal(application.get("institution").get("id"), institutionId));
                }
                if (intakeId != null) {
                    predicates.add(cb.equal(application.get("intake").get("id"), intakeId));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Overdue check-outs (GH-496): students still checked in after their planned stay ended. The
     * bed is free from the end date, so an allocation whose end date is today is already overdue.
     */
    public static Specification<RoomAllocation> overdueCheckOuts(java.time.LocalDate today) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), com.beezhub.student_accommodation.model.enums.AllocationStatus.ACTIVE),
                cb.isNotNull(root.get("stayEndDate")),
                cb.lessThanOrEqualTo(root.get("stayEndDate"), today));
    }
}
