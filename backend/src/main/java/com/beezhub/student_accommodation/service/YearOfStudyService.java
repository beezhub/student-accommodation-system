package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.YearOfStudyNotFoundException;
import com.beezhub.student_accommodation.mapper.YearOfStudyMapper;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.YearOfStudyRequest;
import com.beezhub.student_accommodation.model.dto.YearOfStudyResponse;
import com.beezhub.student_accommodation.model.entity.YearOfStudy;
import com.beezhub.student_accommodation.repository.YearOfStudyRepository;
import com.beezhub.student_accommodation.specification.YearOfStudySpecification;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class YearOfStudyService {
    private final YearOfStudyRepository yearOfStudyRepository;
    private final YearOfStudyMapper yearOfStudyMapper;

    public List<YearOfStudyResponse> getAllYearsOfStudy() {
        List<YearOfStudy> yearOfStudyList = yearOfStudyRepository.findAll();
        return yearOfStudyMapper.toResponseList(yearOfStudyList);
    }

    public PageResponse<YearOfStudyResponse> getAllYearsOfStudy(Pageable pageable) {
        Page<YearOfStudy> page = yearOfStudyRepository.findAll(pageable);
        Page<YearOfStudyResponse> responsePage = page.map(yearOfStudyMapper::toResponse);
        return PageMapper.toPageResponse(responsePage);
    }

    public PageResponse<YearOfStudyResponse> searchYearsOfStudy(Integer year, Pageable pageable) {
        Specification<YearOfStudy> spec = YearOfStudySpecification.filterBy(year);
        Page<YearOfStudy> page = yearOfStudyRepository.findAll(spec, pageable);
        Page<YearOfStudyResponse> responsePage = page.map(yearOfStudyMapper::toResponse);
        return PageMapper.toPageResponse(responsePage);
    }

    public YearOfStudyResponse getYearOfStudyById(Long id) {
        YearOfStudy yearOfStudy = yearOfStudyRepository.findById(id)
                .orElseThrow(() -> new YearOfStudyNotFoundException("Year of study not found"));
        return yearOfStudyMapper.toResponse(yearOfStudy);
    }

    public YearOfStudyResponse createYearOfStudy(YearOfStudyRequest request) {
        YearOfStudy yearOfStudy = new YearOfStudy();
        yearOfStudy.setYearOfStudy(request.getYearOfStudy());
        YearOfStudy saved = yearOfStudyRepository.save(yearOfStudy);
        return yearOfStudyMapper.toResponse(saved);
    }

    public YearOfStudyResponse updateYearOfStudy(Long id, YearOfStudyRequest request) {
        YearOfStudy yearOfStudy = yearOfStudyRepository.findById(id)
                .orElseThrow(() -> new YearOfStudyNotFoundException("Year of study not found"));
        yearOfStudy.setYearOfStudy(request.getYearOfStudy());
        YearOfStudy updated = yearOfStudyRepository.save(yearOfStudy);
        return yearOfStudyMapper.toResponse(updated);
    }

    public void deleteYearOfStudy(Long id) {
        YearOfStudy yearOfStudy = yearOfStudyRepository.findById(id)
                .orElseThrow(() -> new YearOfStudyNotFoundException("Year of study not found"));
        yearOfStudyRepository.delete(yearOfStudy);
    }
}
