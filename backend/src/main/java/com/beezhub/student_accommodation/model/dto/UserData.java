package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserData {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String userRole;
    private Boolean mustChangePassword;
    private Boolean emailVerified;
    private LocalDateTime accountLockedUntil;
    private Integer failedLoginAttempts;
    /** The user's company; 0 is the platform. With a lock, a TENANT_ADMIN on 0 waits for one (#413). */
    private Long tenantId;

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUserRole() {
        return userRole;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }
}
