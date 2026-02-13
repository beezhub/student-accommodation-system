package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.InstitutionNotFoundException;
import com.beezhub.student_accommodation.mapper.InstitutionMapper;
import com.beezhub.student_accommodation.model.dto.InstitutionRequest;
import com.beezhub.student_accommodation.model.dto.InstitutionResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.repository.InstitutionRepository;
import com.beezhub.student_accommodation.specification.InstitutionSpecification;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstitutionService {
    private final InstitutionRepository institutionRepository;
    private final InstitutionMapper institutionMapper;

    public List<InstitutionResponse> getAllInstitutions() {
        List<Institution> institutions = institutionRepository.findAll();
        return institutionMapper.toResponseList(institutions);
    }

    public PageResponse<InstitutionResponse> getAllInstitutions(Pageable pageable) {
        Page<Institution> institutions = institutionRepository.findAll(pageable);
        Page<InstitutionResponse> responsePage = institutions.map(institutionMapper::toResponse);
        return PageMapper.toPageResponse(responsePage);
    }

    public PageResponse<InstitutionResponse> searchInstitutions(String name, String address, String contactEmail, Pageable pageable) {
        Specification<Institution> spec = InstitutionSpecification.filterBy(name, address, contactEmail);
        Page<Institution> institutions = institutionRepository.findAll(spec, pageable);
        Page<InstitutionResponse> responsePage = institutions.map(institutionMapper::toResponse);
        return PageMapper.toPageResponse(responsePage);
    }

    public InstitutionResponse getInstitutionById(Long id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new InstitutionNotFoundException("Institution not found"));
        return institutionMapper.toResponse(institution);
    }

    public InstitutionResponse createInstitution(InstitutionRequest request) {
        Institution institution = new Institution();
        institution.setName(request.getName());
        institution.setAddress(request.getAddress());
        institution.setPhoneNumber(request.getPhoneNumber());
        institution.setEmailAddress(request.getEmailAddress());
        Institution saved = institutionRepository.save(institution);
        return institutionMapper.toResponse(saved);
    }

    public InstitutionResponse updateInstitution(Long id, InstitutionRequest request) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new InstitutionNotFoundException("Institution not found"));
        institution.setName(request.getName());
        institution.setAddress(request.getAddress());
        institution.setPhoneNumber(request.getPhoneNumber());
        institution.setEmailAddress(request.getEmailAddress());
        Institution updated = institutionRepository.save(institution);
        return institutionMapper.toResponse(updated);
    }

    public void deleteInstitution(Long id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new InstitutionNotFoundException("Institution not found"));
        institutionRepository.delete(institution);
    }
}
