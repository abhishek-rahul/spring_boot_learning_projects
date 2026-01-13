package com.flashkart.config;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
public class JwtConfig {

    @Value("${app.security.jwt.secret}")
    private String secret;

    @Bean
    public JwtDecoder jwtDecoder() {
        String s = secret.trim();
        var key = new SecretKeySpec(s.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        String s = secret.trim();
        var key = new SecretKeySpec(s.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        var jwk = new OctetSequenceKey.Builder(key.getEncoded())
                .keyID("flashkart-signing-key")
                .algorithm(JWSAlgorithm.HS256)
                .build();

        var jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }
    
}
