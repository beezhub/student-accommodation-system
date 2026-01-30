package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.AccommodationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccommodationTypeRepository extends JpaRepository<AccommodationType, Long>, JpaSpecificationExecutor<AccommodationType> {

    List<AccommodationType> findByIsActiveTrueOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
