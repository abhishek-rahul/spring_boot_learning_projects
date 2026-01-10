package com.flashkart.identity.service;

import com.flashkart.identity.domain.Role;
import com.flashkart.identity.domain.User;
import com.flashkart.identity.infra.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.flashkart.identity.api.dto.UserResponse;
import java.util.Set;
import java.util.List;
import java.util.UUID;


@Service
public class UserService {

    private static UUID longToUUID(long value) {
        return new UUID(0L, value);
    }

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("USER_ALREADY_EXISTS");
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);

        User user = new User(
                email,
                hashedPassword,
                Set.of(Role.ROLE_USER));

        return userRepository.save(user);
    }

    // ✅ Added for Step 4.6 ABAC/RBAC demo
    public UserResponse getById(UUID userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND"));
        return toResponse(u);
    }

    // ✅ Added for RBAC admin listing
    public List<UserResponse> listAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UserResponse toResponse(User u) {
        // adjust getters if your entity differs
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getRoles());
    }
}
