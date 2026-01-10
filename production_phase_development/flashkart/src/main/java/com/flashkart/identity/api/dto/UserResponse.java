package com.flashkart.identity.api.dto;

import java.util.Set;
import java.util.UUID;
import com.flashkart.identity.domain.Role;


public class UserResponse {
    private UUID id;
    private String email;
    private Set<Role> roles;

    public UserResponse(UUID id, String email, Set<Role> roles) {
        this.id = id;
        this.email = email;
        this.roles = roles;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public Set<Role> getRoles() { return roles; }
}
