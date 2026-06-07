package com.shiviishiv7.matchmaking.common.util.security;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.exception.ErrorResponse;
import com.shiviishiv7.matchmaking.config.CorsConfig;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;

public class JwtAuthenticationFilter implements Filter {

    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON Serializer

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final CorsConfig corsConfig;

    public JwtAuthenticationFilter(CorsConfig corsConfig) {
        this.corsConfig = corsConfig;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("Initializing JwtAuthenticationFilter...");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String authorizationHeader = httpRequest.getHeader("Authorization");
         String token = authorizationHeader;
        if ((authorizationHeader == null || authorizationHeader.isBlank())
                && ((HttpServletRequest) request).getRequestURI().startsWith("/ws")) {
           authorizationHeader =   token = request.getParameter("token");
        }

        String origin = httpRequest.getHeader("Origin");



        log.info("========== Incoming Request ==========");
log.info("Method      : {}", httpRequest.getMethod());
log.info("URL         : {}", httpRequest.getRequestURL());
log.info("Origin      : {}", origin != null ? origin : "NOT PROVIDED");

log.info("Auth Header : {}", authorizationHeader != null ? "PRESENT" : "MISSING");
log.info("======================================");
        applyCorsHeaders(httpResponse, origin); // ← first thing we do

        // Allow OPTIONS requests to pass through without authentication
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            log.info("OPTIONS request detected, passing through...");
            //chain.doFilter(request, response);
              httpResponse.setStatus(HttpServletResponse.SC_OK); // ← immediately return 200
                return; // ← stop here, don't go further
            
        }

        // Validate Authorization Header
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            log.warn("Authorization header is missing or blank.");
            sendUnauthorizedResponse(httpRequest, httpResponse, "Authorization header is missing.");
            return;
        }

        try {
            log.info("Authorization header present. Extracting token...");


//            if (originSource.startsWith("Testpaper7")) {
//                    log.error("Origin source does not start with Testpaper7");
//                    throw new IllegalArgumentException("Invalid : Origin Source");
//            }

            // Decode JWT and extract username
            String username = JwtDecoder.extractUsername(token);
            log.info("Extracted username: {}", username);

            // Set user details in request attributes
            httpRequest.setAttribute("authenticatedUser", username);
            log.info("Set 'authenticatedUser' attribute in request.");

            // Set the current user context
            CurrentUserDetails curr = new CurrentUserDetails();
            curr.setUsername(username);

            CurrentUserContext.setCurrentUser(curr);


            // Proceed with filter chain
            log.info("Proceeding with filter chain...");
            chain.doFilter(request, response);

        } catch (IllegalArgumentException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
            sendUnauthorizedResponse(httpRequest, httpResponse, ex.getMessage());
        }  catch (Exception ex) {
            log.error("Unexpected error during JWT processing: {}", ex.getMessage(), ex);
            sendUnauthorizedResponse(httpRequest, httpResponse, "Internal authentication error.");
        } finally {
            log.info("Clearing current user context.");
            CurrentUserContext.clear();
        }
    }

    @Override
    public void destroy() {
        log.info("Destroying JwtAuthenticationFilter...");
    }

    /**
     * Sends a 401 Unauthorized response.
     */
    private void sendUnauthorizedResponse(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Add CORS headers for frontend access
        applyCorsHeaders(response, request.getHeader("Origin"));

        // Create a structured error response object
        ErrorResponse errorResponse = new ErrorResponse("Unauthorized Access", message, HttpServletResponse.SC_UNAUTHORIZED);

        // Convert ErrorResponse to JSON
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
        response.getWriter().close();

        log.warn("Unauthorized response sent: {}", message);
    }

private void applyCorsHeaders(HttpServletResponse response, String origin) {
    if (origin != null && corsConfig.getAllowedOrigins().contains(origin)) {
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Credentials", "true");
    } else {
        response.setHeader("Access-Control-Allow-Origin", "*");
    }
    // Match exactly what browser sends in access-control-request-headers
    response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
    response.setHeader("Access-Control-Allow-Headers", "authorization, content-type, institutioncode, originsource, accept, origin");
    response.setHeader("Access-Control-Max-Age", "3600");
    log.info("CORS headers applied - Origin: {}", origin);
}
}
