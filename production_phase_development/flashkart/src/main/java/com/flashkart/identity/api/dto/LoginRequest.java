package com.flashkart.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @Email @NotBlank
    public String email;

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @NotBlank
    public String password;

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}