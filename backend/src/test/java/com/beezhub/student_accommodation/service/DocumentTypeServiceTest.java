package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.mapper.DocumentTypeMapper;
import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentTypeServiceTest {

    @Mock
    private DocumentTypeRepository repository;

    @Mock
    private DocumentTypeMapper mapper;

    @InjectMocks
    private DocumentTypeService service;

    private DocumentType dt1;
    private DocumentType dt2;

    @BeforeEach
    void setUp() {
        dt1 = new DocumentType();
        dt1.setId(1L);
        dt1.setTypeName("ID");
        dt1.setIsRequired(true);

        dt2 = new DocumentType();
        dt2.setId(2L);
        dt2.setTypeName("Photo");
        dt2.setIsRequired(false);
    }

    @Test
    void getAllDocumentTypes_shouldMapAll() {
        when(repository.findAll()).thenReturn(List.of(dt1, dt2));
        DocumentTypeResponse r1 = new DocumentTypeResponse();
        r1.setId("1");
        r1.setName("ID");
        DocumentTypeResponse r2 = new DocumentTypeResponse();
        r2.setId("2");
        r2.setName("Photo");
        when(mapper.toDto(dt1)).thenReturn(r1);
        when(mapper.toDto(dt2)).thenReturn(r2);

        List<DocumentTypeResponse> result = service.getAllDocumentTypes();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("ID");
        assertThat(result.get(1).getName()).isEqualTo("Photo");
        verify(repository).findAll();
        verify(mapper, times(1)).toDto(dt1);
        verify(mapper, times(1)).toDto(dt2);
    }

    @Test
    void getRequiredDocumentTypes_shouldMapOnlyRequired() {
        when(repository.findByIsRequiredTrue()).thenReturn(List.of(dt1));
        DocumentTypeResponse r1 = new DocumentTypeResponse();
        r1.setId("1");
        r1.setName("ID");
        when(mapper.toDto(dt1)).thenReturn(r1);

        List<DocumentTypeResponse> result = service.getRequiredDocumentTypes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("ID");
        verify(repository).findByIsRequiredTrue();
        verify(mapper).toDto(dt1);
    }
}
