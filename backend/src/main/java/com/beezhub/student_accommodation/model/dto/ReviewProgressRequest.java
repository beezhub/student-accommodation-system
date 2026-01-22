package com.beezhub.student_accommodation.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for saving document review progress.
 * Allows partial updates to document statuses and comments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewProgressRequest {

    @Valid
    @NotNull(message = "Document updates list is required")
    private List<DocumentUpdate> documentUpdates;

    /**
     * Individual document update request.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentUpdate {

        @NotNull(message = "Document ID is required")
        private Long documentId;

        private String newStatus;

        private String comment;
    }
}
