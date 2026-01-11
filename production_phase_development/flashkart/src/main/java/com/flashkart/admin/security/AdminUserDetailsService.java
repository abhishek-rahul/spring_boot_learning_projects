package com.flashkart.admin.security;

import com.flashkart.identity.infra.UserRepository;
import com.flashkart.identity.domain.User;
import com.flashkart.identity.domain.Role;
import com.flashkart.identity.domain.UserStatus;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import com.flashkart.shared.error.BusinessException; 
import com.flashkart.shared.error.ErrorCode;

import java.util.stream.Collectors;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AdminUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (u.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_ACTIVE, "User is not active", false);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(u.getEmail())
                .password(u.getPasswordHash())
                .authorities(
                        u.getRoles().stream()
                                .map(Role::name)
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toSet())
                )
                .build();
    }
}
