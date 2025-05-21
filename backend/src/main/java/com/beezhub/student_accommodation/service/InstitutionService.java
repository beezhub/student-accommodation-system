package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.mapper.InstitutionMapper;
import com.beezhub.student_accommodation.model.dto.InstitutionResponse;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
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
}
