package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.model.dto.InstitutionResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Institution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BuildingMapper {

    @Mapping(target = "institutions", source = "institutions", qualifiedByName = "institutionsToResponseList")
    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "primaryImageUrl", ignore = true)
    @Mapping(target = "minMonthlyRate", ignore = true)
    @Mapping(target = "maxMonthlyRate", ignore = true)
    @Mapping(target = "availableRoomCount", ignore = true)
    @Mapping(target = "accessibilityFeatures", ignore = true)
    BuildingResponse toResponse(Building building);

    List<BuildingResponse> toResponseList(List<Building> buildings);
    
    @Named("institutionsToResponseList")
    default List<InstitutionResponse> institutionsToResponseList(Set<Institution> institutions) {
        if (institutions == null) {
            return null;
        }
        return institutions.stream()
                .map(this::institutionToResponse)
                .collect(Collectors.toList());
    }
    
    default InstitutionResponse institutionToResponse(Institution institution) {
        if (institution == null) {
            return null;
        }
        InstitutionResponse response = new InstitutionResponse();
        response.setId(institution.getId());
        response.setName(institution.getName());
        response.setAddress(institution.getAddress());
        response.setPhoneNumber(institution.getPhoneNumber());
        response.setEmailAddress(institution.getEmailAddress());
        return response;
    }
}