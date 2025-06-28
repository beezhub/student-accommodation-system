package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.StudentRequest;
import com.beezhub.student_accommodation.model.dto.StudentResponse;
import com.beezhub.student_accommodation.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
    public StudentResponse createStudent(@RequestBody StudentRequest studentRequest) {
        return studentService.createStudent(studentRequest);
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
    public StudentResponse getStudent(@PathVariable Long id) {
        return studentService.getStudentByUserId(id);
    }
}
