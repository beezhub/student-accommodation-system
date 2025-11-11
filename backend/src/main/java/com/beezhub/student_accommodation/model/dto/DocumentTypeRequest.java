package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentTypeRequest {
    @NotBlank(message = "Document type name is required")
    private String name;
    private String description;
    @NotNull(message = "isRequired flag is required")
    private Boolean isRequired;
}
