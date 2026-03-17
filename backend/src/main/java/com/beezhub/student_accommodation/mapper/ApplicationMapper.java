package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.dto.BuildingPreferenceResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.ApplicationBuildingPreference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentNumber", source = "student.studentNumber")
    @Mapping(target = "studentName", expression = "java(application.getStudent().getAppUser().getFirstName() + \" \" + application.getStudent().getAppUser().getLastName())")
    @Mapping(target = "studentEmail", source = "student.appUser.email")
    @Mapping(target = "requiresAccessibility", source = "requiresAccessibility")
    @Mapping(target = "specialRequirements", source = "specialRequirements")
    @Mapping(target = "institutionName", source = "institutionName")
    @Mapping(target = "courseName", source = "courseName")
    @Mapping(target = "preferredBuildingId", source = "preferredBuilding.id")
    @Mapping(target = "preferredBuildingName", source = "preferredBuilding.name")
    @Mapping(target = "allocationMethod", expression = "java(application.getAllocationMethod() != null ? application.getAllocationMethod().name() : null)")
    @Mapping(target = "remarks", source = "remarks")
    @Mapping(target = "preferredBuildings", expression = "java(mapBuildingPreferences(application.getBuildingPreferences()))")
    ApplicationResponse toResponse(Application application);

    default List<BuildingPreferenceResponse> mapBuildingPreferences(
            List<ApplicationBuildingPreference> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return Collections.emptyList();
        }
        return preferences.stream()
                .map(p -> BuildingPreferenceResponse.builder()
                        .buildingId(p.getBuilding().getId())
                        .buildingName(p.getBuilding().getName())
                        .preferenceOrder(p.getPreferenceOrder())
                        .build())
                .collect(Collectors.toList());
    }
}
