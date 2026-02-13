package com.beezhub.student_accommodation.model.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class BatchAllocationResult {
    private int totalAttempted;
    private int successCount;
    private int failureCount;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<Long> allocatedStudentIds = new ArrayList<>();

    @Builder.Default
    private List<Long> allocatedRoomIds = new ArrayList<>();

    private long processingTimeMs;
    private LocalDateTime processedAt;
}
