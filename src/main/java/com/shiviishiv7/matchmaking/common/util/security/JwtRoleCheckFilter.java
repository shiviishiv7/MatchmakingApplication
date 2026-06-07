package com.shiviishiv7.matchmaking.common.util.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtRoleCheckFilter extends OncePerRequestFilter {

    private static final String SECRET_KEY = "your_secret_key"; // This should be configured securely

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Extract the JWT token from the Authorization header
        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);  // Remove the "Bearer " prefix
        }

        // If token is present, validate and check the role
        if (token != null) {
            try {
                Claims claims = validateToken(token);  // Validate JWT token
                String userRole = claims.get("role", String.class);  // Get the user's role from the token

                // Check if the requested path and method match and if the user has the required role
                String path = request.getRequestURI();
                String method = request.getMethod();

                if ("/specific-path".equals(path) && "POST".equals(method)) {
                    if (!"ADMIN".equals(userRole)) {
                        // If the user doesn't have the required role, return 403 Forbidden
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write("You do not have permission to access this resource.");
                        return;  // Don't proceed with the request
                    }
                }

            } catch (SignatureException e) {
                // Token is invalid
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or expired token.");
                return;
            }
        }

        // Continue with the next filter in the chain
        filterChain.doFilter(request, response);
    }

    // Method to validate JWT token and extract claims
    private Claims validateToken(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }
}
