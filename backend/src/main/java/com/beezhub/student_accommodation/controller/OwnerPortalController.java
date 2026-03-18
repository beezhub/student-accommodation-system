package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.OwnerBuildingSummaryDto;
import com.beezhub.student_accommodation.security.user.AppUserDetails;
import com.beezhub.student_accommodation.service.OwnerPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUILDING_OWNER')")
@Tag(name = "Owner Portal", description = "Authenticated building owner portal endpoints")
public class OwnerPortalController {

    private final OwnerPortalService ownerPortalService;

    @Operation(summary = "Get buildings assigned to the authenticated owner")
    @GetMapping("/buildings")
    public List<OwnerBuildingSummaryDto> getMyBuildings(@AuthenticationPrincipal AppUserDetails principal) {
        return ownerPortalService.getOwnerBuildings(principal.getOwnerProfileId());
    }
}
