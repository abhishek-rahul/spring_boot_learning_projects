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

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.flashkart.shared.security.ratelimit.RateLimitFilter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;


@Configuration
@EnableMethodSecurity
public class SecurityConfig {

        @Value("${app.security.jwt.secret}")
        private String secret;

    // -------------------------
    // 1) ADMIN CHAIN (Session + CSRF ON)
    // Matches: /api/v1/admin/**
    // -------------------------
    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurity(HttpSecurity http,
                                             RateLimitFilter rateLimitFilter) throws Exception {

        http
            .securityMatcher("/api/v1/admin/**")
        
            // session-based
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)

            // CSRF ON (browser-safe). Token cookie -> header pattern (XSRF-TOKEN / X-XSRF-TOKEN)
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // login is first touchpoint, allow without csrf
                .ignoringRequestMatchers("/api/v1/admin/auth/login")
            )

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/admin/auth/login",
                    "/api/v1/admin/csrf"
                ).permitAll()
                .anyRequest().hasRole("ADMIN")
            )

            // Admin uses session auth (NOT JWT)
            .httpBasic(Customizer.withDefaults()); // optional fallback (doesn't break session login)

        return http.build();
    }

    // -------------------------
    // 2) API CHAIN (JWT + CSRF OFF, stateless)
    // Matches: /api/**
    // -------------------------
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurity(HttpSecurity http,
                                           RateLimitFilter rateLimitFilter,
                                           ApiAuthEntryPoint entryPoint,
                                           ApiAccessDeniedHandler accessDeniedHandler) throws Exception {

                http
            .securityMatcher("/api/**")
                                // API -> stateless
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .addFilterBefore(rateLimitFilter, BearerTokenAuthenticationFilter.class)
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
                                                .requestMatchers("/api/v1/auth/logout-all").authenticated() // MUST be
                                                                                                            // before
                                                                                                            // permitAll
                                                                                                            // auth/**

                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers("/api/**").authenticated()

                                                .anyRequest().permitAll())

                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(entryPoint)
                                                .accessDeniedHandler(accessDeniedHandler))

                                .oauth2ResourceServer(oauth -> oauth
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));

                return http.build();
        }

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
        private JwtAuthenticationConverter jwtAuthConverter() {
                // We store roles in claim "roles" as "ROLE_USER ROLE_ADMIN"
                JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
                gac.setAuthoritiesClaimName("roles");
                gac.setAuthorityPrefix(""); // because roles already have ROLE_

                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
                converter.setJwtGrantedAuthoritiesConverter(gac);
                return converter;
        }
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
                return cfg.getAuthenticationManager();
        }
}
