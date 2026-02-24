package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.BuildingOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuildingOwnerRepository extends JpaRepository<BuildingOwner, Long> {

    Optional<BuildingOwner> findByUser_Id(Long userId);

    List<BuildingOwner> findByStatus(String status);

    /**
     * Returns all building owner registrations pending platform admin approval.
     */
    default List<BuildingOwner> findPendingRegistrations() {
        return findByStatus("PENDING");
    }
}
