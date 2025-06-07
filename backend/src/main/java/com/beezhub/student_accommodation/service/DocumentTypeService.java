package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.mapper.DocumentTypeMapper;
import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTypeService {

    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentTypeMapper documentTypeMapper;


    public List<DocumentTypeResponse> getAllDocumentTypes() {
        log.info("Fetching all document types");
       List<DocumentTypeResponse> documentTypes = documentTypeRepository.findAll()
                .stream()
                .map(documentTypeMapper::toDto)
                .toList();
        log.info("Found {} document types", documentTypes.size());
        return documentTypes;
    }

    public List<DocumentTypeResponse> getRequiredDocumentTypes() {
        log.info("Fetching required document types");
        List<DocumentTypeResponse> requiredDocumentTypes = documentTypeRepository.findByIsRequiredTrue()
                .stream()
                .map(documentTypeMapper::toDto)
                .toList();
        log.info("Found {} required document types", requiredDocumentTypes.size());
        return requiredDocumentTypes;
    }
}
