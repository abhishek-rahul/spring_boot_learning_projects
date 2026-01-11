package com.flashkart.order.api;

import com.flashkart.shared.api.ApiResponse;
import com.flashkart.shared.observability.CorrelationId;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or @ownership.ownsOrder(#orderId)")
    public ApiResponse<Map<String, Object>> getOrder(@PathVariable Long orderId) {
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        // just demo payload
        return ApiResponse.ok("v1", requestId, Map.of("orderId", orderId));
    }
}
