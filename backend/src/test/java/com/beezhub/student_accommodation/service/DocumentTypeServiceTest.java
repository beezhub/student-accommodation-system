package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DocumentTypeNotFoundException;
import com.beezhub.student_accommodation.mapper.DocumentTypeMapper;
import com.beezhub.student_accommodation.model.dto.DocumentTypeRequest;
import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentTypeServiceTest {

    @Mock
    private DocumentTypeRepository documentTypeRepository;

    @Mock
    private DocumentTypeMapper documentTypeMapper;

    @InjectMocks
    private DocumentTypeService documentTypeService;

    private DocumentType documentType1;
    private DocumentType documentType2;
    private DocumentTypeResponse response1;
    private DocumentTypeResponse response2;
    private DocumentTypeRequest request;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        documentType1 = new DocumentType();
        documentType1.setId(1L);
        documentType1.setTypeName("ID Document");
        documentType1.setDescription("Government issued ID");
        documentType1.setIsRequired(true);

        documentType2 = new DocumentType();
        documentType2.setId(2L);
        documentType2.setTypeName("Proof of Income");
        documentType2.setDescription("Income verification");
        documentType2.setIsRequired(false);

        response1 = new DocumentTypeResponse();
        response1.setId("1");
        response1.setName("ID Document");
        response1.setDescription("Government issued ID");
        response1.setIsRequired(true);

        response2 = new DocumentTypeResponse();
        response2.setId("2");
        response2.setName("Proof of Income");
        response2.setDescription("Income verification");
        response2.setIsRequired(false);

        request = new DocumentTypeRequest();
        request.setName("New Document");
        request.setDescription("New description");
        request.setIsRequired(true);

        pageable = PageRequest.of(0, 20);
    }

    @Test
    void getAllDocumentTypes_ShouldReturnList() {
        // Arrange
        List<DocumentType> documentTypes = Arrays.asList(documentType1, documentType2);
        when(documentTypeRepository.findAll()).thenReturn(documentTypes);
        when(documentTypeMapper.toDto(documentType1)).thenReturn(response1);
        when(documentTypeMapper.toDto(documentType2)).thenReturn(response2);

        // Act
        List<DocumentTypeResponse> result = documentTypeService.getAllDocumentTypes();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(documentTypeRepository).findAll();
    }

    @Test
    void getAllDocumentTypes_WithPageable_ShouldReturnPageResponse() {
        // Arrange
        Page<DocumentType> page = new PageImpl<>(Arrays.asList(documentType1, documentType2), pageable, 2);
        when(documentTypeRepository.findAll(pageable)).thenReturn(page);
        when(documentTypeMapper.toDto(documentType1)).thenReturn(response1);
        when(documentTypeMapper.toDto(documentType2)).thenReturn(response2);

        // Act
        PageResponse<DocumentTypeResponse> result = documentTypeService.getAllDocumentTypes(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        verify(documentTypeRepository).findAll(pageable);
    }

    @Test
    void searchDocumentTypes_WithNameFilter_ShouldReturnFilteredResults() {
        // Arrange
        Page<DocumentType> page = new PageImpl<>(Arrays.asList(documentType1), pageable, 1);
        when(documentTypeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(documentTypeMapper.toDto(documentType1)).thenReturn(response1);

        // Act
        PageResponse<DocumentTypeResponse> result = documentTypeService.searchDocumentTypes("ID", null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("ID Document", result.getContent().get(0).getName());
        verify(documentTypeRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchDocumentTypes_WithIsRequiredTrue_ShouldReturnRequiredDocuments() {
        // Arrange
        Page<DocumentType> page = new PageImpl<>(Arrays.asList(documentType1), pageable, 1);
        when(documentTypeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(documentTypeMapper.toDto(documentType1)).thenReturn(response1);

        // Act
        PageResponse<DocumentTypeResponse> result = documentTypeService.searchDocumentTypes(null, true, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getIsRequired());
        verify(documentTypeRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchDocumentTypes_WithIsRequiredFalse_ShouldReturnOptionalDocuments() {
        // Arrange
        Page<DocumentType> page = new PageImpl<>(Arrays.asList(documentType2), pageable, 1);
        when(documentTypeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(documentTypeMapper.toDto(documentType2)).thenReturn(response2);

        // Act
        PageResponse<DocumentTypeResponse> result = documentTypeService.searchDocumentTypes(null, false, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertFalse(result.getContent().get(0).getIsRequired());
        verify(documentTypeRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchDocumentTypes_WithMultipleFilters_ShouldReturnFilteredResults() {
        // Arrange
        Page<DocumentType> page = new PageImpl<>(Arrays.asList(documentType1), pageable, 1);
        when(documentTypeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(documentTypeMapper.toDto(documentType1)).thenReturn(response1);

        // Act
        PageResponse<DocumentTypeResponse> result = documentTypeService.searchDocumentTypes("ID", true, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(documentTypeRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getRequiredDocumentTypes_ShouldReturnOnlyRequired() {
        // Arrange
        when(documentTypeRepository.findByIsRequiredTrue()).thenReturn(Arrays.asList(documentType1));
        when(documentTypeMapper.toDto(documentType1)).thenReturn(response1);

        // Act
        List<DocumentTypeResponse> result = documentTypeService.getRequiredDocumentTypes();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsRequired());
        verify(documentTypeRepository).findByIsRequiredTrue();
    }

    @Test
    void getDocumentTypeById_ShouldReturnDocumentType() {
        // Arrange
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.of(documentType1));
        when(documentTypeMapper.toDto(documentType1)).thenReturn(response1);

        // Act
        DocumentTypeResponse result = documentTypeService.getDocumentTypeById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("1", result.getId());
        verify(documentTypeRepository).findById(1L);
    }

    @Test
    void getDocumentTypeById_NotFound_ShouldThrowException() {
        // Arrange
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(DocumentTypeNotFoundException.class, () -> documentTypeService.getDocumentTypeById(1L));
    }

    @Test
    void createDocumentType_ShouldSaveAndReturn() {
        // Arrange
        when(documentTypeRepository.save(any(DocumentType.class))).thenReturn(documentType1);
        when(documentTypeMapper.toDto(documentType1)).thenReturn(response1);

        // Act
        DocumentTypeResponse result = documentTypeService.createDocumentType(request);

        // Assert
        assertNotNull(result);
        verify(documentTypeRepository).save(any(DocumentType.class));
    }

    @Test
    void updateDocumentType_ShouldUpdateAndReturn() {
        // Arrange
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.of(documentType1));
        when(documentTypeRepository.save(documentType1)).thenReturn(documentType1);
        when(documentTypeMapper.toDto(documentType1)).thenReturn(response1);

        // Act
        DocumentTypeResponse result = documentTypeService.updateDocumentType(1L, request);

        // Assert
        assertNotNull(result);
        verify(documentTypeRepository).save(documentType1);
    }

    @Test
    void deleteDocumentType_ShouldDelete() {
        // Arrange
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.of(documentType1));

        // Act
        documentTypeService.deleteDocumentType(1L);

        // Assert
        verify(documentTypeRepository).delete(documentType1);
    }
}
