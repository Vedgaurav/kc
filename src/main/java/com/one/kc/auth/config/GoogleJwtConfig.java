package com.one.kc.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class GoogleJwtConfig {

    @Bean
    public JwtDecoder googleJwtDecoder() {

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("User-Agent", "curl/8.0.0");
            request.getHeaders().set("Accept", "application/json");
            return execution.execute(request, body);
        });

        return NimbusJwtDecoder
                .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .restOperations(restTemplate)
                .build();
    }
}

