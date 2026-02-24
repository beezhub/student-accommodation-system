package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.BuildingStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BuildingStaffRepository extends JpaRepository<BuildingStaff, Long> {

    List<BuildingStaff> findByOwner_IdAndIsActiveTrue(Long ownerId);

    List<BuildingStaff> findByUser_Id(Long userId);

    /**
     * Returns active staff invitations that have not yet expired.
     */
    List<BuildingStaff> findByOwner_IdAndIsActiveTrueAndInvitationExpiresAtAfter(
            Long ownerId, LocalDateTime now);
}
