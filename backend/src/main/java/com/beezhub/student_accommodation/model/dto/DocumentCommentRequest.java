package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCommentRequest {

    @NotBlank(message = "Comment text is required")
    @Size(min = 10, message = "Comment must be at least 10 characters long")
    private String commentText;
}
