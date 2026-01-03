package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(source = "documentType.typeName", target = "documentTypeName")
    @Mapping(source = "createdAt", target = "uploadDate", qualifiedByName = "mapLocalDateTimeToLocalDate")
    @Mapping(source = "student.id", target = "studentId")
    @Mapping(source = "student.appUser.firstName", target = "studentFirstName")
    @Mapping(source = "student.appUser.lastName", target = "studentLastName")
    @Mapping(source = "student.appUser.email", target = "studentEmail")
    @Mapping(source = "documentPath", target = "fileUrl")
    @Mapping(target = "applicationId", ignore = true)
    @Mapping(target = "applicationStatus", ignore = true)
    @Mapping(target = "roomAllocated", ignore = true)
    @Mapping(target = "roomId", ignore = true)
    @Mapping(target = "roomNumber", ignore = true)
    DocumentResponse toDocumentResponse(Document document);
    List<DocumentResponse> toDocumentResponseList(List<Document> documents);


    @Named("mapLocalDateTimeToLocalDate")
    default LocalDate mapLocalDateTimeToLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    default DocumentResponse enrichWithApplication(DocumentResponse response, Application application) {
        if (application != null) {
            response.setApplicationId(application.getId());
            response.setApplicationStatus(String.valueOf(application.getStatus()));
        }
        return response;
    }

    default DocumentResponse enrichWithRoom(DocumentResponse response, RoomAllocation allocation) {
        if (allocation != null) {
            response.setRoomAllocated(true);
            if (allocation.getRoom() != null) {
                response.setRoomId(allocation.getRoom().getId());
                response.setRoomNumber(allocation.getRoom().getRoomNumber());
            }
        } else {
            response.setRoomAllocated(false);
        }
        return response;
    }
}
