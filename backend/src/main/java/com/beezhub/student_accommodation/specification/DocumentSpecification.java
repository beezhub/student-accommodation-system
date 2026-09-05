package com.beezhub.student_accommodation.specification;

import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
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
            String studentName,
            Long documentTypeId,
            String status,
            String applicationStatus,
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

            if (studentName != null && !studentName.trim().isEmpty()) {
                String searchTerm = "%" + studentName.toLowerCase() + "%";
                Predicate firstNameMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("student").get("firstName")),
                        searchTerm
                );
                Predicate lastNameMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("student").get("lastName")),
                        searchTerm
                );
                predicates.add(criteriaBuilder.or(firstNameMatch, lastNameMatch));
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

            if (applicationStatus != null && !applicationStatus.trim().isEmpty()) {
                try {
                    ApplicationStatus appStatus = ApplicationStatus.valueOf(applicationStatus.toUpperCase());
                    // Exists subquery: application for this student with desired status
                    var subquery = query.subquery(Long.class);
                    var appRoot = subquery.from(Application.class);
                    subquery.select(appRoot.get("id"))
                            .where(
                                    criteriaBuilder.equal(appRoot.get("student").get("id"), root.get("student").get("id")),
                                    criteriaBuilder.equal(appRoot.get("status"), appStatus)
                            );
                    predicates.add(criteriaBuilder.exists(subquery));
                } catch (IllegalArgumentException ignored) {
                    // invalid status; ignore
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

    /**
     * Limits results to documents of students who have an application in the given tenant.
     * Documents carry no tenant filter of their own, so tenant admins are scoped through the
     * applications they can act on.
     */
    public static Specification<Document> studentHasApplicationInTenant(Long tenantId) {
        return (root, query, criteriaBuilder) -> {
            var subquery = query.subquery(Long.class);
            var appRoot = subquery.from(Application.class);
            subquery.select(appRoot.get("id"))
                    .where(
                            criteriaBuilder.equal(appRoot.get("student").get("id"), root.get("student").get("id")),
                            criteriaBuilder.equal(appRoot.get("tenantId"), tenantId)
                    );
            return criteriaBuilder.exists(subquery);
        };
    }

    public static Specification<Document> belongsToUser(Long userId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("student").get("appUser").get("id"), userId);
    }
}
