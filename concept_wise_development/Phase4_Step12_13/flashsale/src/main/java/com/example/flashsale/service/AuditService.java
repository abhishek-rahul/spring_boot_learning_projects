package com.example.flashsale.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Slf4j
@Service
public class AuditService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCheckoutEvent(String message) {
        // In real world: save to audit table, publish event, etc.
        // For learning: logs prove this commits even when checkout rolls back.
        log.info("[AUDIT] {}", message);
    }
}
