package com.example.chatapp.config;

import com.example.chatapp.security.TokenAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TokenAuthInterceptor tokenAuthInterceptor;

    // Comma-separated list of allowed origins, configurable per environment.
    // Defaults cover the bundled frontend served from the app itself and a
    // typical local dev server port.
    @Value("${app.allowed-origins:http://localhost:8080,http://127.0.0.1:8080}")
    private String[] allowedOrigins;

    public WebConfig(TokenAuthInterceptor tokenAuthInterceptor) {
        this.tokenAuthInterceptor = tokenAuthInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Every /api/rooms/** route requires a valid bearer token.
        // /api/auth/** (register/login) stays open — that's how you get a token.
        registry.addInterceptor(tokenAuthInterceptor)
                .addPathPatterns("/api/rooms/**");
    }
}
