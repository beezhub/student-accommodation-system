package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DuplicateResourceException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.mapper.AccommodationTypeMapper;
import com.beezhub.student_accommodation.model.dto.AccommodationTypeRequest;
import com.beezhub.student_accommodation.model.dto.AccommodationTypeResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.AccommodationType;
import com.beezhub.student_accommodation.repository.AccommodationTypeRepository;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccommodationTypeService {

    private final AccommodationTypeRepository accommodationTypeRepository;
    private final AccommodationTypeMapper accommodationTypeMapper;

    public List<AccommodationTypeResponse> getAllActive() {
        List<AccommodationType> types = accommodationTypeRepository.findByIsActiveTrueOrderByNameAsc();
        return accommodationTypeMapper.toResponseList(types);
    }

    public PageResponse<AccommodationTypeResponse> getAll(Pageable pageable) {
        Page<AccommodationType> page = accommodationTypeRepository.findAll(pageable);
        Page<AccommodationTypeResponse> responsePage = page.map(accommodationTypeMapper::toResponse);
        return PageMapper.toPageResponse(responsePage);
    }

    public AccommodationTypeResponse getById(Long id) {
        AccommodationType type = accommodationTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Accommodation type not found with id: " + id));
        return accommodationTypeMapper.toResponse(type);
    }

    @Transactional
    public AccommodationTypeResponse create(AccommodationTypeRequest request) {
        if (accommodationTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Accommodation type with name '" + request.getName() + "' already exists");
        }

        AccommodationType type = new AccommodationType();
        type.setName(request.getName());
        type.setDescription(request.getDescription());
        type.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        AccommodationType saved = accommodationTypeRepository.save(type);
        return accommodationTypeMapper.toResponse(saved);
    }

    @Transactional
    public AccommodationTypeResponse update(Long id, AccommodationTypeRequest request) {
        AccommodationType type = accommodationTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Accommodation type not found with id: " + id));

        if (accommodationTypeRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Accommodation type with name '" + request.getName() + "' already exists");
        }

        type.setName(request.getName());
        type.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            type.setIsActive(request.getIsActive());
        }

        AccommodationType updated = accommodationTypeRepository.save(type);
        return accommodationTypeMapper.toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        AccommodationType type = accommodationTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Accommodation type not found with id: " + id));
        accommodationTypeRepository.delete(type);
    }
}
