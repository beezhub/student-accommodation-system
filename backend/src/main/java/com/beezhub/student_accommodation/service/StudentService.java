package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.mapper.StudentMapper;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.StudentRequest;
import com.beezhub.student_accommodation.model.dto.StudentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.InstitutionRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.UserRepository;
import com.beezhub.student_accommodation.repository.YearOfStudyRepository;
import com.beezhub.student_accommodation.specification.StudentSpecification;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final YearOfStudyRepository yearOfStudyRepository;

    public StudentResponse createStudent(StudentRequest studentRequest) {
        var entity = studentMapper.toEntity(studentRequest);
        var savedStudent = studentRepository.save(entity);
        return studentMapper.toResponse(savedStudent);
    }

    public StudentResponse getStudentByUserId(Long userId) {
        var current = authService.getCurrentUser();
        // Allow access if admin or requesting own profile
        if (!current.getId().equals(userId) && current.getUserRole() != UserRole.ADMIN) {
            throw new RuntimeException("Unauthorized to access this student");
        }
        Student student = studentRepository.findByAppUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Student not found with user ID: " + userId));
        return studentMapper.toResponse(student);
    }

    public StudentResponse getMyProfile() {
        var current = authService.getCurrentUser();
        Student student = studentRepository.findByAppUser_Id(current.getId())
                .orElseThrow(() -> new RuntimeException("Student profile not found for current user"));
        return studentMapper.toResponse(student);
    }

    @Transactional
    public StudentResponse updateStudent(Long userId, StudentRequest request) {
        var current = authService.getCurrentUser();
        if (!current.getId().equals(userId) && current.getUserRole() != UserRole.ADMIN) {
            throw new RuntimeException("Unauthorized to update this student");
        }
        Student student = studentRepository.findByAppUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Student not found with user ID: " + userId));

        // Update Student fields
        if (request.getStudentNumber() != null) student.setStudentNumber(request.getStudentNumber());
        if (request.getPhoneNumber() != null) student.setPhoneNumber(request.getPhoneNumber());
        if (request.getDateOfBirth() != null) student.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) student.setGender(request.getGender());
        if (request.getSpecialRequirements() != null) student.setSpecialRequirements(request.getSpecialRequirements());

        // Update Institution if changed
        if (request.getInstitutionId() != null) {
            var institution = institutionRepository.findById(request.getInstitutionId())
                    .orElseThrow(() -> new RuntimeException("Institution not found"));
            student.setInstitution(institution);
        }

        // Update Year of Study if changed
        if (request.getYearOfStudyId() != null) {
            var yearOfStudy = yearOfStudyRepository.findById(request.getYearOfStudyId())
                    .orElseThrow(() -> new RuntimeException("Year of study not found"));
            student.setYearOfStudy(yearOfStudy);
        }

        // Update AppUser fields
        AppUser appUser = student.getAppUser();
        if (request.getFirstName() != null) {
            appUser.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            appUser.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(appUser.getEmail())) {
            // Check if email is already taken by another user
            if (userRepository.existsByEmail(request.getEmail()) &&
                !request.getEmail().equals(appUser.getEmail())) {
                throw new IllegalArgumentException("Email already in use");
            }
            appUser.setEmail(request.getEmail());
        }

        // Save both entities
        userRepository.save(appUser);
        var saved = studentRepository.save(student);
        return studentMapper.toResponse(saved);
    }

    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAll().stream().map(studentMapper::toResponse).collect(Collectors.toList());
    }

    public PageResponse<StudentResponse> getAllStudents(Pageable pageable) {
        Page<Student> page = studentRepository.findAll(pageable);
        Page<StudentResponse> responsePage = page.map(studentMapper::toResponse);
        return PageMapper.toPageResponse(responsePage);
    }

    public PageResponse<StudentResponse> searchStudents(String name, String studentNumber, String email, Long institutionId, Long yearOfStudyId, Pageable pageable) {
        Specification<Student> spec = StudentSpecification.filterBy(name, studentNumber, email, institutionId, yearOfStudyId);
        Page<Student> page = studentRepository.findAll(spec, pageable);
        Page<StudentResponse> responsePage = page.map(studentMapper::toResponse);
        return PageMapper.toPageResponse(responsePage);
    }

    public void deleteStudentByUserId(Long userId) {
        studentRepository.findByAppUser_Id(userId).ifPresent(studentRepository::delete);
    }
}
