package com.beezhub.student_accommodation.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OwnerBuildingSummaryDto {
    private Long id;
    private String name;
    private String address;
    private int totalRooms;
    private long occupiedRooms;
}
