package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.AllocationRuleRequest;
import com.beezhub.student_accommodation.model.dto.AllocationRuleResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.service.AllocationRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Allocation Rules", description = "DEPRECATED: Admin endpoints for managing room allocation rules. These endpoints will be removed in a future version.")
@RestController
@RequestMapping("/api/admin/allocation-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Deprecated(since = "2.0", forRemoval = true)
public class AllocationRuleController {

    private final AllocationRuleService allocationRuleService;

    @Operation(
            summary = "List allocation rules",
            description = "Lists all allocation rules for a specific building or global rules (buildingId = null). " +
                          "Supports pagination with page, size, and sort parameters."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rules retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Building not found")
    })
    @GetMapping
    public ResponseEntity<PageResponse<AllocationRuleResponse>> getRules(
            @Parameter(description = "Building ID (null for global rules)")
            @RequestParam(required = false) Long buildingId,
            @PageableDefault(size = 20, sort = "priority") Pageable pageable) {
        PageResponse<AllocationRuleResponse> response = allocationRuleService.getRulesByBuilding(buildingId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get allocation rule by ID",
            description = "Retrieves a single allocation rule by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @GetMapping("/{ruleId}")
    public ResponseEntity<AllocationRuleResponse> getRuleById(
            @Parameter(description = "ID of the allocation rule", required = true)
            @PathVariable Long ruleId) {
        AllocationRuleResponse response = allocationRuleService.getRuleById(ruleId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Create allocation rule",
            description = "Creates a new allocation rule. Weights must sum to 100%. " +
                          "Rule names must be unique within the same building scope."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rule created successfully"),
            @ApiResponse(responseCode = "400", description = "Weights don't sum to 100% or validation failed"),
            @ApiResponse(responseCode = "404", description = "Building not found"),
            @ApiResponse(responseCode = "409", description = "Rule name already exists for this building")
    })
    @PostMapping
    public ResponseEntity<AllocationRuleResponse> createRule(
            @Valid @RequestBody AllocationRuleRequest request,
            Authentication authentication) {
        AllocationRuleResponse response = allocationRuleService.createRule(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Update allocation rule",
            description = "Updates an existing allocation rule. Weights must sum to 100%."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule updated successfully"),
            @ApiResponse(responseCode = "400", description = "Weights don't sum to 100% or validation failed"),
            @ApiResponse(responseCode = "404", description = "Rule or building not found"),
            @ApiResponse(responseCode = "409", description = "Rule name already exists for this building")
    })
    @PutMapping("/{ruleId}")
    public ResponseEntity<AllocationRuleResponse> updateRule(
            @Parameter(description = "ID of the allocation rule", required = true)
            @PathVariable Long ruleId,
            @Valid @RequestBody AllocationRuleRequest request,
            Authentication authentication) {
        AllocationRuleResponse response = allocationRuleService.updateRule(ruleId, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete allocation rule",
            description = "Soft deletes an allocation rule by setting active = false. " +
                          "The rule can be reactivated using the activate endpoint."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule deleted (deactivated) successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Map<String, Boolean>> deleteRule(
            @Parameter(description = "ID of the allocation rule", required = true)
            @PathVariable Long ruleId,
            Authentication authentication) {
        allocationRuleService.deleteRule(ruleId, authentication.getName());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @Operation(
            summary = "Activate allocation rule",
            description = "Activates an allocation rule by setting active = true."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule activated successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @PatchMapping("/{ruleId}/activate")
    public ResponseEntity<AllocationRuleResponse> activateRule(
            @Parameter(description = "ID of the allocation rule", required = true)
            @PathVariable Long ruleId,
            Authentication authentication) {
        AllocationRuleResponse response = allocationRuleService.activateRule(ruleId, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
