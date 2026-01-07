package com.flashkart.shared.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "FlashKart API",
                version = "v1",
                description = "Production-grade e-commerce platform APIs"
        )
)
public class OpenApiConfig {
}
