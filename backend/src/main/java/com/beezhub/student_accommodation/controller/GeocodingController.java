package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.service.geocoding.AddressSuggestion;
import com.beezhub.student_accommodation.service.geocoding.GeocodingService;
import com.beezhub.student_accommodation.service.geocoding.ResolvedPlace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    /**
     * Place ids and session tokens are URL-safe base64 and a UUID. Anything else is refused here
     * rather than forwarded into a URL at the provider.
     */
    private static final String PLACE_ID = "[A-Za-z0-9_-]{1,1024}";
    private static final String SESSION_TOKEN = "[A-Za-z0-9_-]{1,36}";

    private final GeocodingService geocodingService;

    @Operation(summary = "Suggest addresses for a partly typed query")
    @ApiResponse(responseCode = "200", description = "Suggestions, best first; empty if none match")
    @ApiResponse(responseCode = "503", description = "Geocoding is not configured or is unreachable")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<List<AddressSuggestion>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "session", required = false) @Pattern(regexp = SESSION_TOKEN) String session) {
        // The query is an address someone is typing; only its length is logged.
        log.info("action=geocodingSearch queryLength={}", query == null ? 0 : query.length());
        return ResponseEntity.ok(geocodingService.search(query, session));
    }

    @Operation(summary = "Coordinates of a picked suggestion")
    @ApiResponse(responseCode = "200", description = "The place's coordinates")
    @ApiResponse(responseCode = "503", description = "Geocoding is not configured or is unreachable")
    @GetMapping("/places/{placeId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ResolvedPlace> resolve(
            @PathVariable @Pattern(regexp = PLACE_ID) String placeId,
            @RequestParam(value = "session", required = false) @Pattern(regexp = SESSION_TOKEN) String session) {
        log.info("action=geocodingResolve");
        return ResponseEntity.ok(geocodingService.resolve(placeId, session));
    }
}
