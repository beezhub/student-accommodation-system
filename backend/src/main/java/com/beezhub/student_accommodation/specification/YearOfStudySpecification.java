package com.beezhub.student_accommodation.specification;

import com.beezhub.student_accommodation.model.entity.YearOfStudy;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class YearOfStudySpecification {

    public static Specification<YearOfStudy> filterBy(Integer year) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (year != null) {
                predicates.add(criteriaBuilder.equal(root.get("year"), year));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
