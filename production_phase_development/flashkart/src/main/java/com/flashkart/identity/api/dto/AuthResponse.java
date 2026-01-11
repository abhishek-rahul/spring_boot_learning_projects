package com.flashkart.identity.api.dto;

public class AuthResponse {
    private UserResponse userResponse;
    private TokenResponse tokenResponse;

    public AuthResponse(UserResponse userResponse, TokenResponse tokenResponse) {
        this.userResponse = userResponse;
        this.tokenResponse = tokenResponse;
    }

    public UserResponse getUserResponse() { return userResponse; }
    public TokenResponse getTokenResponse() { return tokenResponse; }
}