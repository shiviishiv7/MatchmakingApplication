package com.shiviishiv7.matchmaking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable) // allow H2 console
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/h2-console/**",               // H2 browser console
                    "/ws/**",                       // WebSocket handshake
                    "/company/search/signup",       // unauthenticated signup search
                    "/actuator/**",
                    "/swagger-ui/**",               // Swagger UI
                    "/swagger-ui.html",
                    "/v3/api-docs/**",              // OpenAPI JSON/YAML
                    "/v3/api-docs.yaml",
                    "/reddit/**"                    // Reddit proxy — public, no auth needed
                ).permitAll()
                .anyRequest().permitAll()           // open during dev — tighten before prod
            );

        return http.build();
    }
}
