package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DuplicateResourceException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.mapper.AccommodationTypeMapper;
import com.beezhub.student_accommodation.model.dto.AccommodationTypeRequest;
import com.beezhub.student_accommodation.model.dto.AccommodationTypeResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.AccommodationType;
import com.beezhub.student_accommodation.repository.AccommodationTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccommodationTypeServiceTest {

    @Mock private AccommodationTypeRepository accommodationTypeRepository;
    @Mock private AccommodationTypeMapper accommodationTypeMapper;

    @InjectMocks
    private AccommodationTypeService service;

    private AccommodationType type;
    private AccommodationTypeResponse response;

    @BeforeEach
    void setUp() {
        type = new AccommodationType();
        type.setId(1L);
        type.setName("Studio");
        type.setDescription("Studio apartment");
        type.setIsActive(true);

        response = new AccommodationTypeResponse();
    }

    @Test
    void getAllActive_returnsActiveTypes() {
        when(accommodationTypeRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(type));
        when(accommodationTypeMapper.toResponseList(List.of(type))).thenReturn(List.of(response));

        List<AccommodationTypeResponse> result = service.getAllActive();

        assertThat(result).hasSize(1);
        verify(accommodationTypeRepository).findByIsActiveTrueOrderByNameAsc();
    }

    @Test
    void getAll_returnsPagedResult() {
        Page<AccommodationType> page = new PageImpl<>(List.of(type));
        when(accommodationTypeRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(accommodationTypeMapper.toResponse(type)).thenReturn(response);

        PageResponse<AccommodationTypeResponse> result = service.getAll(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        verify(accommodationTypeRepository).findAll(any(PageRequest.class));
    }

    @Test
    void getById_existingId_returnsResponse() {
        when(accommodationTypeRepository.findById(1L)).thenReturn(Optional.of(type));
        when(accommodationTypeMapper.toResponse(type)).thenReturn(response);

        AccommodationTypeResponse result = service.getById(1L);

        assertThat(result).isNotNull();
    }

    @Test
    void getById_nonExistingId_throwsException() {
        when(accommodationTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_uniqueName_savesAndReturns() {
        AccommodationTypeRequest request = new AccommodationTypeRequest();
        request.setName("Studio");
        request.setDescription("A studio unit");
        request.setIsActive(true);

        when(accommodationTypeRepository.existsByName("Studio")).thenReturn(false);
        when(accommodationTypeRepository.save(any(AccommodationType.class))).thenReturn(type);
        when(accommodationTypeMapper.toResponse(type)).thenReturn(response);

        AccommodationTypeResponse result = service.create(request);

        assertThat(result).isNotNull();
        verify(accommodationTypeRepository).save(any(AccommodationType.class));
    }

    @Test
    void create_nullIsActive_defaultsToTrue() {
        AccommodationTypeRequest request = new AccommodationTypeRequest();
        request.setName("Suite");
        request.setIsActive(null);

        when(accommodationTypeRepository.existsByName("Suite")).thenReturn(false);
        when(accommodationTypeRepository.save(any(AccommodationType.class))).thenAnswer(inv -> {
            AccommodationType saved = inv.getArgument(0);
            assertThat(saved.getIsActive()).isTrue();
            return saved;
        });
        when(accommodationTypeMapper.toResponse(any())).thenReturn(response);

        service.create(request);
    }

    @Test
    void create_duplicateName_throwsException() {
        AccommodationTypeRequest request = new AccommodationTypeRequest();
        request.setName("Studio");

        when(accommodationTypeRepository.existsByName("Studio")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(accommodationTypeRepository, never()).save(any());
    }

    @Test
    void update_validRequest_updatesAndReturns() {
        AccommodationTypeRequest request = new AccommodationTypeRequest();
        request.setName("Suite");
        request.setDescription("Updated");
        request.setIsActive(false);

        when(accommodationTypeRepository.findById(1L)).thenReturn(Optional.of(type));
        when(accommodationTypeRepository.existsByNameAndIdNot("Suite", 1L)).thenReturn(false);
        when(accommodationTypeRepository.save(type)).thenReturn(type);
        when(accommodationTypeMapper.toResponse(type)).thenReturn(response);

        AccommodationTypeResponse result = service.update(1L, request);

        assertThat(result).isNotNull();
        assertThat(type.getName()).isEqualTo("Suite");
        assertThat(type.getIsActive()).isFalse();
    }

    @Test
    void update_nonExistingId_throwsException() {
        when(accommodationTypeRepository.findById(99L)).thenReturn(Optional.empty());

        AccommodationTypeRequest request = new AccommodationTypeRequest();
        request.setName("Suite");

        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_duplicateName_throwsException() {
        AccommodationTypeRequest request = new AccommodationTypeRequest();
        request.setName("Suite");

        when(accommodationTypeRepository.findById(1L)).thenReturn(Optional.of(type));
        when(accommodationTypeRepository.existsByNameAndIdNot("Suite", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void update_nullIsActive_doesNotChangeActiveStatus() {
        AccommodationTypeRequest request = new AccommodationTypeRequest();
        request.setName("Studio");
        request.setIsActive(null);

        type.setIsActive(true);
        when(accommodationTypeRepository.findById(1L)).thenReturn(Optional.of(type));
        when(accommodationTypeRepository.existsByNameAndIdNot("Studio", 1L)).thenReturn(false);
        when(accommodationTypeRepository.save(type)).thenReturn(type);
        when(accommodationTypeMapper.toResponse(type)).thenReturn(response);

        service.update(1L, request);

        assertThat(type.getIsActive()).isTrue(); // unchanged
    }

    @Test
    void delete_existingId_deletesType() {
        when(accommodationTypeRepository.findById(1L)).thenReturn(Optional.of(type));

        service.delete(1L);

        verify(accommodationTypeRepository).delete(type);
    }

    @Test
    void delete_nonExistingId_throwsException() {
        when(accommodationTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(accommodationTypeRepository, never()).delete(any(AccommodationType.class));
    }
}
