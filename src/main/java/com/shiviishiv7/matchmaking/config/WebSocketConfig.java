package com.shiviishiv7.matchmaking.config;

import com.shiviishiv7.matchmaking.common.util.security.JwtDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Server pushes to clients on these prefixes
        registry.enableSimpleBroker("/topic", "/queue");
        // Client sends to server on /app prefix
        registry.setApplicationDestinationPrefixes("/app");
        // Private messages use /user/{userId}/queue/...
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(jwtHandshakeInterceptor())
                .setHandshakeHandler(principalHandshakeHandler())
                .withSockJS();
    }

    /**
     * Extracts the JWT sub from the Authorization header or token query param
     * during the WebSocket handshake and stores it as a session attribute.
     */
    private HandshakeInterceptor jwtHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler wsHandler, Map<String, Object> attributes) {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    String token = servletRequest.getServletRequest().getParameter("token");
                    if (token == null) {
                        token = servletRequest.getServletRequest().getHeader("Authorization");
                    }
                    if (token != null && !token.isBlank()) {
                        try {
                            String sub = JwtDecoder.extractUsername(token);
                            attributes.put("sub", sub);
                            log.info("WebSocket handshake authenticated for sub: {}", sub);
                        } catch (Exception ex) {
                            log.error("ALERT_FOR_ERROR: WebSocket handshake JWT decode failed: {}", ex.getMessage());
                            return false;
                        }
                    } else {
                        log.warn("WebSocket handshake missing token — rejecting.");
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {}
        };
    }

    /**
     * Creates a Spring Principal from the sub stored in the handshake attributes.
     * This enables /user/{sub}/queue/... private messaging.
     */
    private DefaultHandshakeHandler principalHandshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                              Map<String, Object> attributes) {
                String sub = (String) attributes.get("sub");
                if (sub == null) return null;
                return () -> sub;  // Principal.getName() returns the Cognito sub
            }
        };
    }
}
