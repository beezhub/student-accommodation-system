package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.mapper.StudentMapper;
import com.beezhub.student_accommodation.model.dto.StudentRequest;
import com.beezhub.student_accommodation.model.dto.StudentResponse;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    public StudentResponse createStudent(StudentRequest studentRequest) {
        var entity = studentMapper.toEntity(studentRequest);
        var savedStudent = studentRepository.save(entity);
        return studentMapper.toResponse(savedStudent);
    }

    public StudentResponse getStudentByUserId(Long userId) {
        Student student = studentRepository.findByAppUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Student not found with user ID: " + userId));
        return studentMapper.toResponse(student);
    }
}
