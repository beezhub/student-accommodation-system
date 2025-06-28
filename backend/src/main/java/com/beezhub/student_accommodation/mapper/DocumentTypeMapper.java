package com.beezhub.student_accommodation.mapper;


import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentTypeMapper {

    @Mapping(source = "typeName", target = "name")
    DocumentTypeResponse toDto(DocumentType documentType);

    List<DocumentTypeResponse> toDtoList(List<DocumentType> documentTypes);
}
