package com.flashkart.identity.api.dto;

public class TokenResponse {
    public String accessToken;
    public String refreshToken;

    public TokenResponse(String a, String r) {
        this.accessToken = a;
        this.refreshToken = r;
    }
}