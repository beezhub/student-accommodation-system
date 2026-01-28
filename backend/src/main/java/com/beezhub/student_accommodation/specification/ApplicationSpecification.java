package com.beezhub.student_accommodation.specification;

import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.StagedAllocation;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ApplicationSpecification {

    public static Specification<Application> filterBy(String applicationCode, String studentName,
            ApplicationStatus status, String allocationStatus, String preferredBuilding,
            LocalDate startDate, LocalDate endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (applicationCode != null && !applicationCode.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("applicationCode")),
                        "%" + applicationCode.toLowerCase() + "%"));
            }

            if (studentName != null && !studentName.trim().isEmpty()) {
                Predicate firstNamePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("student").get("appUser").get("firstName")),
                        "%" + studentName.toLowerCase() + "%");
                Predicate lastNamePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("student").get("appUser").get("lastName")),
                        "%" + studentName.toLowerCase() + "%");
                predicates.add(criteriaBuilder.or(firstNamePredicate, lastNamePredicate));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (allocationStatus != null && !allocationStatus.trim().isEmpty()) {
                // Allocation status can come from StagedAllocation or Application status
                // ALLOCATED - application status is ALLOCATED
                // PENDING - application is AWAITING_ALLOCATION but no staged allocation or
                // staged is PROPOSED
                // REJECTED - staged allocation status is REJECTED
                if ("ALLOCATED".equalsIgnoreCase(allocationStatus)) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), ApplicationStatus.ALLOCATED));
                } else if ("PENDING".equalsIgnoreCase(allocationStatus)) {
                    // Either AWAITING_ALLOCATION status or PENDING application status
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("status"), ApplicationStatus.AWAITING_ALLOCATION),
                            criteriaBuilder.equal(root.get("status"), ApplicationStatus.PENDING)));
                } else if ("REJECTED".equalsIgnoreCase(allocationStatus)) {
                    // Application rejected status
                    predicates.add(criteriaBuilder.equal(root.get("status"), ApplicationStatus.REJECTED));
                }
            }

            if (preferredBuilding != null && !preferredBuilding.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("preferredBuilding").get("name")),
                        "%" + preferredBuilding.toLowerCase() + "%"));
            }

            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("applicationDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("applicationDate"), endDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
