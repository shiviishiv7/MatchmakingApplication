package com.shiviishiv7.matchmaking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Matchmaking API")
                        .version("1.0.0")
                        .description("Backend API for the professional matchmaking platform. " +
                                "Handles user onboarding, company lookup, matching, meetings, WebRTC signaling and feedback.")
                        .contact(new Contact()
                                .name("Shivji Prasad")
                                .email("shivji@example.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local dev server")
                ))
                .tags(List.of(
                        new Tag().name("Company").description("Company search and management"),
                        new Tag().name("User").description("User registration and profile"),
                        new Tag().name("User Preference").description("Match preference settings"),
                        new Tag().name("Match").description("Match creation and lifecycle"),
                        new Tag().name("Meeting").description("Meeting scheduling and status"),
                        new Tag().name("Meeting Feedback").description("Post-meeting feedback submission"),
                        new Tag().name("WebRTC").description("ICE server credentials for WebRTC"),
                        new Tag().name("Instant Match").description("Real-time WebSocket matching")
                ));
    }
}
