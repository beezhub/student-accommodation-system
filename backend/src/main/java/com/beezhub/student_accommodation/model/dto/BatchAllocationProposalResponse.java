package com.beezhub.student_accommodation.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BatchAllocationProposalResponse {

    private String batchId;
    private Integer totalStudents;
    private Integer successfulProposals;
    private Integer failedProposals;
    private LocalDateTime createdAt;
    private Long createdById;
    private String createdByName;

    @Builder.Default
    private List<StagedAllocationResponse> proposals = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Builder.Default
    private List<FailedProposal> failures = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedProposal {
        private Long studentId;
        private String studentName;
        private String reason;
    }

    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }

    public void addFailure(Long studentId, String studentName, String reason) {
        if (failures == null) {
            failures = new ArrayList<>();
        }
        failures.add(FailedProposal.builder()
                .studentId(studentId)
                .studentName(studentName)
                .reason(reason)
                .build());
    }
}
