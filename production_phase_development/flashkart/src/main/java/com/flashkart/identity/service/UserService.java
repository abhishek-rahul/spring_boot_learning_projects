package com.flashkart.identity.service;

import com.flashkart.identity.domain.Role;
import com.flashkart.identity.domain.User;
import com.flashkart.identity.infra.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserService {

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
                Set.of(Role.ROLE_USER)
        );

        return userRepository.save(user);
    }
}
