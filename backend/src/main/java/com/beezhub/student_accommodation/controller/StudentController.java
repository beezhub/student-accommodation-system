package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.StudentRequest;
import com.beezhub.student_accommodation.model.dto.StudentResponse;
import com.beezhub.student_accommodation.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@Tag(name = "Student", description = "Student Management APIs" )
public class StudentController {

    private final StudentService studentService;

    @Operation(
            summary = "Create Student",
            description = "Create a new student"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Student created successfully"
    )
        @PostMapping
        @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
        public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentRequest studentRequest) {
                var response = studentService.createStudent(studentRequest);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get Student",
            description = "Retrieve student details by ID"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Student details retrieved successfully"
    )
        @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
        public ResponseEntity<StudentResponse> getStudent(@PathVariable Long id) {
                return ResponseEntity.ok(studentService.getStudentByUserId(id));
        }

        @GetMapping("/me")
        @Operation(
                        summary = "Get my student profile",
                        description = "Retrieve the student profile for the authenticated user"
        )
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
        @PreAuthorize("hasRole('STUDENT')")
        public ResponseEntity<StudentResponse> getMyProfile() {
                return ResponseEntity.ok(studentService.getMyProfile());
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update student profile")
        @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
        public ResponseEntity<StudentResponse> updateStudent(@PathVariable Long id,
                                                                                                                 @Valid @RequestBody StudentRequest request) {
                return ResponseEntity.ok(studentService.updateStudent(id, request));
        }

        @GetMapping
        @Operation(summary = "List all students (admin)", description = "Returns list of students. Supports pagination with query params: page, size, sort")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<?> listStudents(
                        @RequestParam(required = false) Boolean paginated,
                        @PageableDefault(size = 20, sort = "id") Pageable pageable) {
                if (Boolean.TRUE.equals(paginated)) {
                        PageResponse<StudentResponse> page = studentService.getAllStudents(pageable);
                        return ResponseEntity.ok(page);
                }
                return ResponseEntity.ok(studentService.getAllStudents());
        }

        @GetMapping("/search")
        @Operation(summary = "Search students (admin)", description = "Search and filter students by name, student number, email, institution, or year of study")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<PageResponse<StudentResponse>> searchStudents(
                        @RequestParam(required = false) String name,
                        @RequestParam(required = false) String studentNumber,
                        @RequestParam(required = false) String email,
                        @RequestParam(required = false) Long institutionId,
                        @RequestParam(required = false) Long yearOfStudyId,
                        @PageableDefault(size = 20, sort = "id") Pageable pageable) {
                PageResponse<StudentResponse> page = studentService.searchStudents(name, studentNumber, email, institutionId, yearOfStudyId, pageable);
                return ResponseEntity.ok(page);
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete student (admin)")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
                studentService.deleteStudentByUserId(id);
                return ResponseEntity.noContent().build();
        }
}
