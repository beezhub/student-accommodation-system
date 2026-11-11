package com.beezhub.student_accommodation.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for document details with nested comments.
 * Used in ApplicationFullDetailsResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentWithCommentsResponse {

    private Long id;
    private String fileName;
    private String type;
    private String status;
    private LocalDateTime uploadedAt;
    private Long documentTypeId;
    private String documentTypeName;

    /** For an expiring type, the last day it is valid at the application's company (GH-498). */
    private java.time.LocalDate expiryDate;
    private Boolean expired;

    private List<CommentResponse> comments;

    /**
     * Nested comment response for documents.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentResponse {
        private Long id;
        private String adminName;
        private String text;
        private LocalDateTime createdAt;
    }
}
