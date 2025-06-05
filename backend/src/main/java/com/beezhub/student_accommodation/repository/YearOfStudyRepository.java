package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.YearOfStudy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YearOfStudyRepository extends JpaRepository<YearOfStudy, Long> {


}
