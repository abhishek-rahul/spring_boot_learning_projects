package com.flashkart.identity.service;

//import com.flashkart.identity.domain.IdentityErrorCodes;
import com.flashkart.shared.error.ErrorCode;
import com.flashkart.identity.domain.User;
import com.flashkart.identity.infra.UserRepository;
import com.flashkart.shared.error.BusinessException; // adjust package if your BusinessException path differs
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, UserService userService, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public User signup(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Email already registered", false);
        }
        return userService.createUser(email, rawPassword);
    }

    public User login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found", false));

        // if you want: blocked check
        // if (user.getStatus() == UserStatus.BLOCKED) throw new BusinessException(...)

        boolean ok = passwordEncoder.matches(rawPassword, user.getPasswordHash());
        if (!ok) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password", false);
        }

        return user;
    }
}
