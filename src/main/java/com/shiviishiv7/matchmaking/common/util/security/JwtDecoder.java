package com.shiviishiv7.matchmaking.common.util.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JwtDecoder {

    private static final Logger log = LoggerFactory.getLogger(JwtDecoder.class);
    private static final ObjectMapper objectMapper = new ObjectMapper(); // Reuse ObjectMapper

    public static String extractUsername(String token) throws JsonProcessingException {
        log.info("Starting JWT extraction process...");

        if (token == null || token.isBlank()) {
            log.error("JWT token is null or empty");
            throw new IllegalArgumentException("Invalid JWT token: Token is empty");
        }

        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            log.error("Invalid JWT token: Not enough parts");
            throw new IllegalArgumentException("Invalid JWT token: Incorrect format");
        }

            // Decode payload
            String payloadJson = decodeBase64(parts[1]);
            log.debug("Decoded JWT payload: {}", payloadJson);

            // Parse JSON claims
            Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);

            // Extract username
            String username = Optional.ofNullable(claims.get("sub"))
                    .map(Object::toString)
                    .orElseThrow(() -> new IllegalArgumentException("JWT token missing 'sub' claim"));

            // Extract roles
            //List<String> roles = (List<String>) claims.getOrDefault("cognito:groups", List.of());
           // log.debug("Extracted roles: {}", roles);

            // Validate user role based on origin header
          //  validateUserRole(originSource, roles);


            log.info("JWT extraction successful. Username: {}", username);

            return username;

    }

    private static String decodeBase64(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    private static void validateUserRole(String originSource, List<String> roles) {
        if (originSource == null || originSource.isBlank()) {
            log.error("Missing origin header, rejecting JWT token");
            throw new IllegalArgumentException("Invalid : Missing origin header");
        }

        log.debug("Checking originHeader conditions: {}", originSource);

        if (originSource.startsWith("Admin") || originSource.startsWith("Testpaper7")) {
            if (!roles.contains("admin")) {
                log.error("JWT token does not have 'admin' role, rejecting request");
                throw new IllegalArgumentException("Invalid : Missing 'admin' role");
            }
        } else if (originSource.startsWith("Student")) {
            if (!roles.contains("student")) {  // Fixed incorrect condition
                log.error("JWT token does not have 'student' role, rejecting request");
                throw new IllegalArgumentException("Invalid : Missing 'student' role");
            }
        } else {
            log.error("Unexpected origin header format, rejecting JWT token");
            throw new IllegalArgumentException("Invalid JWT token: Unrecognized origin");
        }
    }
}
