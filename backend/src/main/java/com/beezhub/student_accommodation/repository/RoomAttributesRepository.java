package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.RoomAttributes;
import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomAttributesRepository extends JpaRepository<RoomAttributes, Long> {

    Optional<RoomAttributes> findByRoom_Id(Long roomId);

    List<RoomAttributes> findByAccessibilityCompliantTrue();

    List<RoomAttributes> findByGenderRestriction(GenderRestriction genderRestriction);

    List<RoomAttributes> findByRoom_Building_Id(Long buildingId);
}
