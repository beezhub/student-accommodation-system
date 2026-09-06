package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class BuildingRequest {
    @NotBlank(message = "Building name is required")
    private String name;

    private String address;

    private String placeId;

    private Double latitude;

    private Double longitude;

    private String description;

    @Min(value = 1, message = "Total floors must be at least 1")
    private Integer totalFloors;

    private List<Long> institutionIds;

    /**
     * The property company that will own the building.
     *
     * <p>Required when a platform admin creates a building, ignored otherwise. Buildings are the
     * root of the ownership chain — a room derives its tenant from its building, an allocation from
     * its room — so there is nothing to derive a building's own tenant from. It used to be left to
     * TenantListener, which copies the acting context; a platform admin's context is 0, so the
     * building was stamped with the platform tenant and became invisible to the company that owns
     * it, with no column from which a backfill could repair it (GH-213).
     *
     * <p>A tenant admin does not send this: their own tenant is the only one they may create in,
     * and a value here is refused rather than silently ignored.
     */
    private Long tenantId;
}