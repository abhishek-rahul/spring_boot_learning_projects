package com.flashkart.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.flashkart.shared.security.ratelimit.RateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(RateLimitProperties.class)
@Configuration
public class SecurityBeansConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper().findAndRegisterModules();
        // IMPORTANT: don't close servlet output stream
        om.getFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        return om;
    }
}