package com.beezhub.student_accommodation.specification;

import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BuildingBrowseSpecification {

    public static Specification<Building> filterBy(
            BigDecimal minPrice, BigDecimal maxPrice,
            Boolean available, Long institutionId, String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Ensure distinct results when joining
            query.distinct(true);

            // Price range filter: building must have at least one room with monthly_rate in range
            if (minPrice != null || maxPrice != null) {
                Subquery<Long> priceSubquery = query.subquery(Long.class);
                var roomRoot = priceSubquery.from(Room.class);
                List<Predicate> roomPredicates = new ArrayList<>();
                roomPredicates.add(cb.equal(roomRoot.get("building"), root));
                roomPredicates.add(cb.isNotNull(roomRoot.get("monthlyRate")));

                if (minPrice != null) {
                    roomPredicates.add(cb.greaterThanOrEqualTo(roomRoot.get("monthlyRate"), minPrice));
                }
                if (maxPrice != null) {
                    roomPredicates.add(cb.lessThanOrEqualTo(roomRoot.get("monthlyRate"), maxPrice));
                }

                priceSubquery.select(roomRoot.get("id"))
                        .where(cb.and(roomPredicates.toArray(new Predicate[0])));
                predicates.add(cb.exists(priceSubquery));
            }

            // Availability filter: the building must have at least one room with space left.
            //
            // Derived from the live allocation count rather than r.status = 'AVAILABLE'. That
            // string is set by RoomOccupancyManager.deriveStatus, which says AVAILABLE only when a
            // room is completely empty, so a two-bed room with one occupant read as full and its
            // building disappeared from the filter despite having a free bed (GH-221).
            if (Boolean.TRUE.equals(available)) {
                Subquery<Long> availSubquery = query.subquery(Long.class);
                var roomRoot = availSubquery.from(Room.class);

                Subquery<Long> heldBeds = availSubquery.subquery(Long.class);
                var allocationRoot = heldBeds.from(RoomAllocation.class);
                heldBeds.select(cb.count(allocationRoot))
                        .where(
                                cb.equal(allocationRoot.get("room"), roomRoot),
                                allocationRoot.get("status").in(
                                        AllocationStatus.ACTIVE,
                                        AllocationStatus.ACCEPTED,
                                        AllocationStatus.OFFERED)
                        );

                availSubquery.select(roomRoot.get("id"))
                        .where(
                                cb.equal(roomRoot.get("building"), root),
                                cb.notEqual(roomRoot.get("status"), "MAINTENANCE"),
                                cb.greaterThan(roomRoot.<Integer>get("capacity").as(Long.class), heldBeds)
                        );
                predicates.add(cb.exists(availSubquery));
            }

            // Institution filter: building must be associated with the given institution
            if (institutionId != null) {
                Join<Object, Object> institutionJoin = root.join("institutions", JoinType.INNER);
                predicates.add(cb.equal(institutionJoin.get("id"), institutionId));
            }

            // Text search (name or address)
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("address")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
