package com.beezhub.student_accommodation.specification;

import com.beezhub.student_accommodation.model.entity.Student;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class StudentSpecification {

    public static Specification<Student> filterBy(String name, String studentNumber, String email, Long institutionId, Long yearOfStudyId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.trim().isEmpty()) {
                Predicate firstNamePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("appUser").get("firstName")),
                        "%" + name.toLowerCase() + "%"
                );
                Predicate lastNamePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("appUser").get("lastName")),
                        "%" + name.toLowerCase() + "%"
                );
                predicates.add(criteriaBuilder.or(firstNamePredicate, lastNamePredicate));
            }

            if (studentNumber != null && !studentNumber.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("studentNumber")),
                        "%" + studentNumber.toLowerCase() + "%"
                ));
            }

            if (email != null && !email.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.join("appUser").get("email")),
                        "%" + email.toLowerCase() + "%"
                ));
            }

            if (institutionId != null) {
                predicates.add(criteriaBuilder.equal(root.get("institution").get("id"), institutionId));
            }

            if (yearOfStudyId != null) {
                predicates.add(criteriaBuilder.equal(root.get("yearOfStudy").get("id"), yearOfStudyId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
