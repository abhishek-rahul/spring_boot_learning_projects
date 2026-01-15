package com.flashkart.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashkart.order.domain.IdempotencyKey;
import com.flashkart.order.infra.IdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing idempotency keys to ensure safe retry of checkout requests.
 * Uses SHA-256 hashing to store idempotency keys securely.
 */
@Service
public class IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    private static final int IDEMPOTENCY_KEY_EXPIRY_HOURS = 24;

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository, ObjectMapper objectMapper) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Hash an idempotency key using SHA-256.
     */
    public String hashKey(String idempotencyKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(idempotencyKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Check if an idempotency key already exists and return the stored response if found.
     * Returns empty if the key doesn't exist or has expired.
     */
    @Transactional(readOnly = true)
    public Optional<IdempotencyResponse> getExistingResponse(String idempotencyKey, UUID userId, String requestPath) {
        String keyHash = hashKey(idempotencyKey);
        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByKeyHash(keyHash);

        if (existing.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyKey key = existing.get();
        
        // Validate user and path match
        if (!key.getUserId().equals(userId) || !key.getRequestPath().equals(requestPath)) {
            logger.warn("Idempotency key hash collision or mismatch: userId={}, path={}", userId, requestPath);
            return Optional.empty();
        }

        // Check if expired
        if (key.isExpired()) {
            logger.debug("Idempotency key expired: {}", keyHash);
            return Optional.empty();
        }

        // Return stored response
        return Optional.of(new IdempotencyResponse(
            key.getResponseStatusCode(),
            key.getResponseBody()
        ));
    }

    /**
     * Store the idempotency key and response for future duplicate requests.
     */
    @Transactional
    public void storeResponse(String idempotencyKey, UUID userId, String requestPath, 
                             Integer statusCode, Object responseBody) {
        try {
            String keyHash = hashKey(idempotencyKey);
            Instant expiresAt = Instant.now().plus(IDEMPOTENCY_KEY_EXPIRY_HOURS, ChronoUnit.HOURS);

            IdempotencyKey key = new IdempotencyKey(keyHash, userId, requestPath, expiresAt);
            key.setResponseStatusCode(statusCode);
            
            // Serialize response body to JSON
            if (responseBody != null) {
                String jsonBody = objectMapper.writeValueAsString(responseBody);
                key.setResponseBody(jsonBody);
            }

            idempotencyKeyRepository.save(key);
            logger.debug("Stored idempotency key response: keyHash={}, statusCode={}", keyHash, statusCode);
        } catch (Exception e) {
            logger.error("Failed to store idempotency key response", e);
            // Don't fail the request if idempotency storage fails
        }
    }

    /**
     * Response wrapper for idempotency key lookups.
     */
    public static class IdempotencyResponse {
        private final Integer statusCode;
        private final String responseBody;

        public IdempotencyResponse(Integer statusCode, String responseBody) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public Integer getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }
}
