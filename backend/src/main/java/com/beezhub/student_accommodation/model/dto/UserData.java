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
}
