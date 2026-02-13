package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
