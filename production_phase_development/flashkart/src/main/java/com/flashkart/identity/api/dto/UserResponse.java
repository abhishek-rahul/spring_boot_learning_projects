package com.flashkart.identity.api.dto;

import java.util.Set;
import java.util.UUID;

public class UserResponse {
    private UUID id;
    private String email;
    private Set<String> roles;

    public UserResponse(UUID id, String email, Set<String> roles) {
        this.id = id;
        this.email = email;
        this.roles = roles;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public Set<String> getRoles() { return roles; }
}
