package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private UserData user;

    public String getToken() {
        return token;
    }

    public UserData getUser() {
        return user;
    }
}
