package com.beezhub.student_accommodation.specification;

import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DocumentSpecification {

    public static Specification<Document> filterBy(
            String documentName,
            Long studentId,
            Long documentTypeId,
            String status,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (documentName != null && !documentName.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("documentName")),
                        "%" + documentName.toLowerCase() + "%"
                ));
            }

            if (studentId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("student").get("id"),
                        studentId
                ));
            }

            if (documentTypeId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("documentType").get("id"),
                        documentTypeId
                ));
            }

            if (status != null && !status.trim().isEmpty()) {
                try {
                    DocumentStatus documentStatus = DocumentStatus.valueOf(status.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("documentStatus"), documentStatus));
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore this filter
                }
            }

            if (createdAfter != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        createdAfter
                ));
            }

            if (createdBefore != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"),
                        createdBefore
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Document> belongsToUser(Long userId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("student").get("appUser").get("id"), userId);
    }
}
