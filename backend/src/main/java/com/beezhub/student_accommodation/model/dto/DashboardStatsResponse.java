package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private Long totalApplications;
    private Long pendingApplications;
    private Long approvedApplications;
    private Long rejectedApplications;
    private Long totalDocuments;
    private Long pendingDocuments;
    private Long approvedDocuments;
    private Long rejectedDocuments;
    private Long totalStudents;
    private Long totalAdmins;
}
