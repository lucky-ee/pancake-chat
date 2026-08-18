package com.example.chatapp.config;

import com.example.chatapp.security.AuthHandshakeInterceptor;
import com.example.chatapp.security.CustomHandshakeHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthHandshakeInterceptor authHandshakeInterceptor;
    private final CustomHandshakeHandler customHandshakeHandler;

    @Value("${app.allowed-origins:http://localhost:8080,http://127.0.0.1:8080}")
    private String[] allowedOrigins;

    public WebSocketConfig(AuthHandshakeInterceptor authHandshakeInterceptor,
                            CustomHandshakeHandler customHandshakeHandler) {
        this.authHandshakeInterceptor = authHandshakeInterceptor;
        this.customHandshakeHandler = customHandshakeHandler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Only known origins may open a socket — closes off cross-site
                // WebSocket hijacking from arbitrary third-party pages.
                .setAllowedOrigins(allowedOrigins)
                // Validates the bearer token during the HTTP handshake, before
                // the socket ever opens.
                .addInterceptors(authHandshakeInterceptor)
                // Binds the validated username as this session's Principal.
                .setHandshakeHandler(customHandshakeHandler)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
