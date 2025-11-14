package com.beezhub.student_accommodation.specification;

import com.beezhub.student_accommodation.model.entity.DocumentType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class DocumentTypeSpecification {

    public static Specification<DocumentType> filterBy(String name, Boolean isRequired) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("typeName")),
                        "%" + name.toLowerCase() + "%"
                ));
            }

            if (isRequired != null) {
                predicates.add(criteriaBuilder.equal(root.get("isRequired"), isRequired));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
