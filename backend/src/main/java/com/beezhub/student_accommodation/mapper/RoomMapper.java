package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.RoomResponse;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {BuildingMapper.class})
public interface RoomMapper {

    @Mapping(target = "buildingId", source = "building.id")
    @Mapping(target = "buildingName", source = "building.name")
    @Mapping(target = "accessibilityCompliant", source = "room", qualifiedByName = "mapAccessibility")
    @Mapping(target = "genderRestriction", source = "room", qualifiedByName = "mapGenderRestriction")
    @Mapping(target = "desirabilityOverride", source = "room", qualifiedByName = "mapDesirability")
    @Mapping(target = "minYearOfStudy", source = "room", qualifiedByName = "mapMinYearOfStudy")
    @Mapping(target = "maxYearOfStudy", source = "room", qualifiedByName = "mapMaxYearOfStudy")
    RoomResponse toResponse(Room room);

    List<RoomResponse> toResponseList(List<Room> rooms);

    @Named("mapAccessibility")
    default Boolean mapAccessibility(Room room) {
        if (room.getAttributes() == null) {
            return false;
        }
        Boolean accessible = room.getAttributes().getAccessibilityCompliant();
        return accessible != null ? accessible : false;
    }

    @Named("mapGenderRestriction")
    default GenderRestriction mapGenderRestriction(Room room) {
        if (room.getAttributes() == null || room.getAttributes().getGenderRestriction() == null) {
            return GenderRestriction.ANY;
        }
        return room.getAttributes().getGenderRestriction();
    }

    @Named("mapDesirability")
    default Integer mapDesirability(Room room) {
        // Desirability override removed - always return null
        return null;
    }

    @Named("mapMinYearOfStudy")
    default Integer mapMinYearOfStudy(Room room) {
        if (room.getAttributes() == null) {
            return null;
        }
        return room.getAttributes().getMinYearOfStudy();
    }

    @Named("mapMaxYearOfStudy")
    default Integer mapMaxYearOfStudy(Room room) {
        if (room.getAttributes() == null) {
            return null;
        }
        return room.getAttributes().getMaxYearOfStudy();
    }
}
