package com.flashkart.identity.api;

import com.flashkart.identity.api.dto.UserResponse;
import com.flashkart.identity.service.UserService;
import com.flashkart.shared.api.ApiResponse;
import com.flashkart.shared.observability.CorrelationId;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.parameters.P;

import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // RBAC: admin can fetch anyone
    // ABAC: user can fetch self
    @GetMapping("/{userId}")
    //@PreAuthorize("hasRole('ADMIN')")
    @PreAuthorize("hasRole('ROLE_ADMIN') or @ownership.isSelf(#userId)")
    public ApiResponse<UserResponse> getUser(@P("userId") @PathVariable UUID userId) {
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        return ApiResponse.ok("v1", requestId, userService.getById(userId));
    }

    // RBAC: Only admin can list all users
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<List<UserResponse>> listUsers() {
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        return ApiResponse.ok("v1", requestId, userService.listAll());
    }
}
