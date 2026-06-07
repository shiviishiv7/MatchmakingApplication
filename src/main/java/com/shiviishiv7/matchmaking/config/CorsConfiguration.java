package com.shiviishiv7.matchmaking.config;


import com.shiviishiv7.matchmaking.common.util.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfiguration {

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:4200",                                      // local dev
            "https://main.d10sxmiyt52yv8.amplifyapp.com",               // AWS Amplify
            "https://shallweconnect.online",                             // production domain
            "https://www.shallweconnect.online"                          // www
    );

    @Bean
    public CorsConfig corsConfig() {
        return new CorsConfig(ALLOWED_ORIGINS);
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(ALLOWED_ORIGINS.toArray(new String[0]))
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilter() {
        FilterRegistrationBean<JwtAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new JwtAuthenticationFilter(corsConfig()));
        registrationBean.addUrlPatterns("/*"); // Apply to all URL patterns
        registrationBean.setOrder(1); // Set precedence
        return registrationBean;
    }
}
