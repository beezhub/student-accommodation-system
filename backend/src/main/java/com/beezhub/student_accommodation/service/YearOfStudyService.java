package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.mapper.YearOfStudyMapper;
import com.beezhub.student_accommodation.model.dto.YearOfStudyResponse;
import com.beezhub.student_accommodation.model.entity.YearOfStudy;
import com.beezhub.student_accommodation.repository.YearOfStudyRepository;
import lombok.RequiredArgsConstructor;
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
}
