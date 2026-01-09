package com.flashkart.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class TokenHashService {

    private final String pepper;

    public TokenHashService(@Value("${app.security.refresh-token.pepper}") String pepper) {
        this.pepper = pepper;
    }

    public String sha256Hex(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((token + pepper).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("TOKEN_HASH_FAILED", e);
        }
    }
}
