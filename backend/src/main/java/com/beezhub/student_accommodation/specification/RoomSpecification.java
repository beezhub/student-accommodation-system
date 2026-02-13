package com.beezhub.student_accommodation.specification;

import com.beezhub.student_accommodation.model.entity.Room;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class RoomSpecification {

    public static Specification<Room> filterBy(String roomNumber, Long buildingId, Integer minCapacity, Integer maxCapacity) {
        return (root, query, criteriaBuilder) -> {
            // Add fetch joins to avoid N+1 queries
            if (query.getResultType() == Room.class) {
                root.fetch("building", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("attributes", jakarta.persistence.criteria.JoinType.LEFT);
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (roomNumber != null && !roomNumber.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("roomNumber")),
                        "%" + roomNumber.toLowerCase() + "%"
                ));
            }

            if (buildingId != null) {
                predicates.add(criteriaBuilder.equal(root.get("building").get("id"), buildingId));
            }

            if (minCapacity != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("capacity"), minCapacity));
            }

            if (maxCapacity != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("capacity"), maxCapacity));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
