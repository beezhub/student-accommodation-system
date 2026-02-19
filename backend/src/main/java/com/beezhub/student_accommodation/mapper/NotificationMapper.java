package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.NotificationDto;
import com.beezhub.student_accommodation.model.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {
    NotificationDto toDto(Notification notification);
}
