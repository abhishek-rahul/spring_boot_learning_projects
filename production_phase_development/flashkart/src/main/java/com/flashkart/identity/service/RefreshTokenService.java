package com.flashkart.identity.service;

import com.flashkart.identity.domain.RefreshToken;
import com.flashkart.identity.domain.User;
import com.flashkart.shared.error.ErrorCode;
import com.flashkart.identity.infra.RefreshTokenRepository;
import com.flashkart.identity.infra.UserRepository;
import com.flashkart.shared.error.BusinessException; // adjust to your package
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final TokenHashService hashService;
    private final UserRepository userRepository;
    private final long ttlDays;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository repo,
            TokenHashService hashService,
            UserRepository userRepository,
            @Value("${app.security.refresh-token.ttl-days}") long ttlDays
    ) {
        this.repo = repo;
        this.hashService = hashService;
        this.userRepository = userRepository;
        this.ttlDays = ttlDays;
    }

    public IssuedRefreshToken issue(UUID userId, String ip, String userAgent) {
        String raw = generateOpaqueToken();
        String hash = hashService.sha256Hex(raw);

        Instant now = Instant.now();
        Instant exp = now.plus(ttlDays, ChronoUnit.DAYS);

        RefreshToken rt = new RefreshToken(userId, hash, now, exp, ip, userAgent);
        repo.save(rt);

        return new IssuedRefreshToken(raw, exp);
    }

    /**
     * Rotation:
     * - validate old token
     * - revoke old token (replaced_by = new token id)
     * - issue new refresh token
     */
    public IssuedRefreshToken rotate(String rawRefreshToken, String ip, String userAgent) {
        String hash = hashService.sha256Hex(rawRefreshToken);

        RefreshToken existing = repo.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid refresh token", false));

        if (existing.isRevoked() || existing.isExpired()) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Refresh token expired/revoked" ,false);
        }
//BusinessException(ErrorCode errorCode, String message, boolean retryable)
        IssuedRefreshToken newOne = issue(existing.getUserId(), ip, userAgent);

        // revoke old with replacedBy = new token's id (we don't have id here; optional)
        existing.revoke(null);
        repo.save(existing);

        return newOne;
    }

    public UUID getUserIdFromRefresh(String rawRefreshToken) {
        String hash = hashService.sha256Hex(rawRefreshToken);
        RefreshToken rt = repo.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid refresh token", false));

        if (rt.isRevoked() || rt.isExpired()) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Refresh token expired/revoked" ,false);
        }
        return rt.getUserId();
    }

    public void revoke(String rawRefreshToken) {
        String hash = hashService.sha256Hex(rawRefreshToken);
        repo.findByTokenHash(hash).ifPresent(rt -> {
            if (!rt.isRevoked()) {
                rt.revoke(null);
                repo.save(rt);
            }
        });
    }

    public void revokeAllForUser(UUID userId) {
        List<RefreshToken> tokens = repo.findAllByUserIdAndRevokedAtIsNull(userId);
        for (RefreshToken rt : tokens) rt.revoke(null);
        repo.saveAll(tokens);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48]; // 64-ish chars base64url
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Get user from refresh token
     * Extracted from AuthController to maintain Clean Architecture
     */
    public User getUserFromRefresh(String rawRefreshToken) {
        UUID userId = getUserIdFromRefresh(rawRefreshToken);
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User Not Found", false));
    }

    /**
     * Rotate refresh token and return both token and user
     * Extracted from AuthController to maintain Clean Architecture
     */
    public record RefreshTokenWithUser(IssuedRefreshToken token, User user) {}

    public RefreshTokenWithUser rotateWithUser(String rawRefreshToken, String ip, String userAgent) {
        UUID userId = getUserIdFromRefresh(rawRefreshToken);
        IssuedRefreshToken newToken = rotate(rawRefreshToken, ip, userAgent);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User Not Found", false));
        return new RefreshTokenWithUser(newToken, user);
    }

    public record IssuedRefreshToken(String refreshToken, Instant expiresAt) {}
}
