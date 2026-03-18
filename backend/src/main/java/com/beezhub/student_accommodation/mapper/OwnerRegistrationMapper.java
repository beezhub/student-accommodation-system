package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.OwnerRegistrationResponse;
import com.beezhub.student_accommodation.model.dto.OwnerRegistrationSummaryDto;
import com.beezhub.student_accommodation.model.entity.OwnerRegistration;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OwnerRegistrationMapper {

    OwnerRegistrationResponse toResponse(OwnerRegistration registration);

    OwnerRegistrationSummaryDto toSummary(OwnerRegistration registration);
}
