package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.OwnerBuildingSummaryDto;
import com.beezhub.student_accommodation.security.jwt.JwtUtil;
import com.beezhub.student_accommodation.service.OwnerPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final JwtUtil jwtUtil;

    @Operation(summary = "Get buildings assigned to the authenticated owner")
    @GetMapping("/buildings")
    public List<OwnerBuildingSummaryDto> getMyBuildings(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7) : null;
        Long ownerProfileId = token != null ? jwtUtil.extractOwnerProfileId(token) : null;
        return ownerPortalService.getOwnerBuildings(ownerProfileId);
    }
}
