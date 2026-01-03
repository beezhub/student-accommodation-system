package com.beezhub.student_accommodation.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class DocumentResponse {
    private Long id;
    private String documentName;
    private String documentTypeName;
    private String fileUrl;
    private String documentStatus;
    private LocalDate uploadDate;
    // Application context
    private Long applicationId;
    private String applicationStatus;
    // Student context
    private Long studentId;
    private String studentFirstName;
    private String studentLastName;
    private String studentEmail;
    // Allocation context
    private Boolean roomAllocated;
    private Long roomId;
    private String roomNumber;
}
