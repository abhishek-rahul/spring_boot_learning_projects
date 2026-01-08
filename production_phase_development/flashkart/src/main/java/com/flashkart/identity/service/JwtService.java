package com.flashkart.identity.service;

import com.flashkart.identity.domain.User;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long ttlMinutes;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.access-token-ttl-minutes}") long ttlMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.ttlMinutes = ttlMinutes;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(ttlMinutes, ChronoUnit.MINUTES);

        String roles = user.getRoles()
                .stream()
                .map(Enum::name)
                .collect(Collectors.joining(" ")); // "ROLE_USER ROLE_ADMIN"

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(exp)
                .subject(user.getId().toString()) // subject = userId
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .keyId("flashkart-signing-key")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();    }

    public long accessTokenTtlSeconds() {
        return ttlMinutes * 60;
    }
}
