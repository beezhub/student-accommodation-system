package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.CreateUserRequest;
import com.beezhub.student_accommodation.model.dto.MessageResponse;
import com.beezhub.student_accommodation.model.dto.UserData;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserData> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserData user = userManagementService.createUser(request);
        return ResponseEntity.status(201).body(user);
    }

    @Operation(summary = "Get all users")
    @ApiResponse(responseCode = "200", description = "Users retrieved")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserData>> getAllUsers() {
        return ResponseEntity.ok(userManagementService.getAllUsers());
    }

    @Operation(summary = "Get users by role")
    @ApiResponse(responseCode = "200", description = "Users retrieved")
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserData>> getUsersByRole(@PathVariable String role) {
        UserRole userRole = UserRole.enumRoleOf(role);
        return ResponseEntity.ok(userManagementService.getUsersByRole(userRole));
    }

    @Operation(summary = "Delete user")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userManagementService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unlock user account")
    @ApiResponse(responseCode = "200", description = "Account unlocked")
    @PutMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> unlockAccount(@PathVariable Long id) {
        authService.unlockAccount(id);
        return ResponseEntity.ok(new MessageResponse("Account unlocked successfully"));
    }
}
