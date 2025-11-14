package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DocumentTypeNotFoundException;
import com.beezhub.student_accommodation.mapper.DocumentTypeMapper;
import com.beezhub.student_accommodation.model.dto.DocumentTypeRequest;
import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.specification.DocumentTypeSpecification;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    public PageResponse<DocumentTypeResponse> getAllDocumentTypes(Pageable pageable) {
        log.info("Fetching paginated document types");
        Page<DocumentType> page = documentTypeRepository.findAll(pageable);
        Page<DocumentTypeResponse> responsePage = page.map(documentTypeMapper::toDto);
        log.info("Found {} document types in page", page.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public PageResponse<DocumentTypeResponse> searchDocumentTypes(String name, Boolean isRequired, Pageable pageable) {
        log.info("Searching document types with filters - name: {}, isRequired: {}", name, isRequired);
        Specification<DocumentType> spec = DocumentTypeSpecification.filterBy(name, isRequired);
        Page<DocumentType> page = documentTypeRepository.findAll(spec, pageable);
        Page<DocumentTypeResponse> responsePage = page.map(documentTypeMapper::toDto);
        log.info("Found {} document types matching criteria", page.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
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

    public DocumentTypeResponse getDocumentTypeById(Long id) {
        DocumentType documentType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new DocumentTypeNotFoundException("Document type not found"));
        return documentTypeMapper.toDto(documentType);
    }

    public DocumentTypeResponse createDocumentType(DocumentTypeRequest request) {
        DocumentType documentType = new DocumentType();
        documentType.setTypeName(request.getName());
        documentType.setDescription(request.getDescription());
        documentType.setIsRequired(request.getIsRequired());
        DocumentType saved = documentTypeRepository.save(documentType);
        return documentTypeMapper.toDto(saved);
    }

    public DocumentTypeResponse updateDocumentType(Long id, DocumentTypeRequest request) {
        DocumentType documentType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new DocumentTypeNotFoundException("Document type not found"));
        documentType.setTypeName(request.getName());
        documentType.setDescription(request.getDescription());
        documentType.setIsRequired(request.getIsRequired());
        DocumentType updated = documentTypeRepository.save(documentType);
        return documentTypeMapper.toDto(updated);
    }

    public DocumentTypeResponse updateRequiredFlag(Long id, boolean required) {
        DocumentType documentType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new DocumentTypeNotFoundException("Document type not found"));
        documentType.setIsRequired(required);
        DocumentType updated = documentTypeRepository.save(documentType);
        return documentTypeMapper.toDto(updated);
    }

    public void deleteDocumentType(Long id) {
        DocumentType documentType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new DocumentTypeNotFoundException("Document type not found"));
        documentTypeRepository.delete(documentType);
    }
}
