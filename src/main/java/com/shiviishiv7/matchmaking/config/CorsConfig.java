package com.shiviishiv7.matchmaking.config;

import java.util.List;

public class CorsConfig {
    private final List<String> allowedOrigins;

    public CorsConfig(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }
}
