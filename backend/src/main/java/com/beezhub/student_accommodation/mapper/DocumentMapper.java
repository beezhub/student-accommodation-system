package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(source = "documentType.typeName", target = "documentTypeName")
    @Mapping(source = "createdAt", target = "uploadDate")
    DocumentResponse toDocumentResponse(Document document);
    List<DocumentResponse> toDocumentResponseList(List<Document> documents);
}
