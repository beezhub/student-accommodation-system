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

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}