package com.beezhub.student_accommodation.model.enums;

import lombok.*;

import java.util.Arrays;


@ToString
@Getter
public enum UserRole {
    STUDENT("Student"),
    ADMIN("Admin"),
    LANDLORD("Landlord");

    private final String role;

    UserRole(String role) {
        this.role = role;
    }

    public static UserRole enumRoleOf(String role){
        return Arrays.stream(UserRole.values())
                .filter(userRole -> userRole.getRole().equalsIgnoreCase(role))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + role));
    }



}
