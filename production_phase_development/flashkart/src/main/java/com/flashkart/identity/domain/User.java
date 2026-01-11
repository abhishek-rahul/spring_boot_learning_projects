package com.flashkart.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(columnNames = "email")
)
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, name = "password_hash")
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role")
    private Set<Role> roles;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {}

    public User(String email, String passwordHash, Set<Role> roles) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.status = UserStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UserStatus getStatus() {
        return status;
    }

    public String getEmail() {
        return email;
    }

    public Set<Role> getRoles() {
        return roles;
    }
    public String getPasswordHash() {
        return passwordHash;
    }

    
    // getters only (immutability mindset)
}
