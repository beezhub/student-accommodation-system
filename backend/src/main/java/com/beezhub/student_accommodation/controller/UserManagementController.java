package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.CreateUserRequest;
import com.beezhub.student_accommodation.model.dto.MessageResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.UserData;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.UserManagementService;
import com.beezhub.student_accommodation.util.PageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Admin-only user management APIs")
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final AuthService authService;

    @Operation(
        summary = "Create new admin or landlord user",
        description = "Admin-only endpoint to create ADMIN or LANDLORD users"
    )
    @ApiResponse(responseCode = "201", description = "User created successfully")
    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<UserData> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserData user = userManagementService.createUser(request);
        return ResponseEntity.status(201).body(user);
    }

    @Operation(summary = "Get all users, one page at a time")
    @ApiResponse(responseCode = "200", description = "Users retrieved")
    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<PageResponse<UserData>> getAllUsers(
            @PageableDefault(size = 25, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(
                PageMapper.toPageResponse(userManagementService.getAllUsers(pageable)));
    }

    @Operation(summary = "Get users by role, one page at a time")
    @ApiResponse(responseCode = "200", description = "Users retrieved")
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<PageResponse<UserData>> getUsersByRole(
            @PathVariable String role,
            @PageableDefault(size = 25, sort = "id") Pageable pageable) {
        UserRole userRole = UserRole.enumRoleOf(role);
        return ResponseEntity.ok(
                PageMapper.toPageResponse(userManagementService.getUsersByRole(userRole, pageable)));
    }

    @Operation(summary = "Delete user")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userManagementService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unlock user account")
    @ApiResponse(responseCode = "200", description = "Account unlocked")
    @PutMapping("/{id}/unlock")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<MessageResponse> unlockAccount(@PathVariable Long id) {
        authService.unlockAccount(id);
        return ResponseEntity.ok(new MessageResponse("Account unlocked successfully"));
    }
}
