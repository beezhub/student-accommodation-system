package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.service.geocoding.AddressSuggestion;
import com.beezhub.student_accommodation.service.geocoding.GeocodingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Address lookup for the building form (GH-152).
 *
 * <p>Admin-only, and not because the data is sensitive: every call costs money at the provider, so
 * an unauthenticated endpoint here is a way to spend someone else's budget. Only the roles that can
 * create or edit a building have any use for it.
 */
@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Geocoding", description = "Address lookup for building addresses")
public class GeocodingController {

    private final GeocodingService geocodingService;

    @Operation(summary = "Find candidate addresses for a query")
    @ApiResponse(responseCode = "200", description = "Candidates, best first; empty if none match")
    @ApiResponse(responseCode = "503", description = "Geocoding is not configured or is unreachable")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<List<AddressSuggestion>> search(@RequestParam("q") String query) {
        // The query is an address someone is typing; only its length is logged.
        log.info("action=geocodingSearch queryLength={}", query == null ? 0 : query.length());
        return ResponseEntity.ok(geocodingService.search(query));
    }
}
