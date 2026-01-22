package com.beezhub.student_accommodation.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequiredDocumentsChecklistResponse {

    private Long applicationId;
    private Integer totalRequired;
    private Integer approved;
    private Integer pending;
    private Integer rejected;
    private Integer notUploaded;
    private List<DocumentChecklistItem> documents;
    private Boolean allApproved;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentChecklistItem {
        private Long documentTypeId;
        private String type;
        private String status;
        private LocalDateTime uploadedAt;
        private Long documentId;
        private String documentName;
        private Boolean isRequired;
    }

    /**
     * Calculates whether all required documents are approved.
     */
    public void calculateAllApproved() {
        this.allApproved = totalRequired != null && approved != null &&
                           totalRequired > 0 && approved.equals(totalRequired);
    }
}
