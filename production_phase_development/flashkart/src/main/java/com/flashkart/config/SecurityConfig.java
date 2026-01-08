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

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.security.jwt.secret}")
    private String secret;

    @Bean
    public SecurityFilterChain apiSecurity(HttpSecurity http,
            ApiAuthEntryPoint entryPoint,
            ApiAccessDeniedHandler accessDeniedHandler) throws Exception {

        http
                // API -> stateless
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));

        return http.build();
    }

    /*
     * @Bean
     * public JwtDecoder jwtDecoder() {
     * // HS256 secret key
     * var key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
     * return NimbusJwtDecoder.withSecretKey(key).build();
     * }
     */

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
    

    /*
     * @Bean
     * public JwtEncoder jwtEncoder() {
     * var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
     * "HmacSHA256");
     * var jwk = new OctetSequenceKey.Builder(key.getEncoded())
     * .keyID("flashkart-signing-key")
     * .algorithm(com.nimbusds.jose.JWSAlgorithm.HS256) // Add this line
     * .build();
     * var jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
     * return new NimbusJwtEncoder(jwks);
     * }
     */

    private JwtAuthenticationConverter jwtAuthConverter() {
        // We store roles in claim "roles" as "ROLE_USER ROLE_ADMIN"
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("roles");
        gac.setAuthorityPrefix(""); // because roles already have ROLE_

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(gac);
        return converter;
    }
}
