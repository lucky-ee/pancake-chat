package com.example.chatapp.security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

/**
 * Runs during the initial HTTP handshake that upgrades to a WebSocket
 * connection (before any STOMP frames are exchanged). The client passes its
 * bearer token as a query param: /ws?token=<token>
 *
 * If the token is missing or invalid, the handshake is rejected outright
 * (HTTP 401) — the socket never opens. If valid, the resolved username is
 * stashed in the WebSocket session attributes so CustomHandshakeHandler can
 * turn it into a real Principal for the lifetime of the session.
 */
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final TokenStore tokenStore;

    public AuthHandshakeInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        Optional<String> username = tokenStore.validate(token);

        if (username.isEmpty()) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false; // abort the handshake — connection is refused
        }

        attributes.put("username", username.get());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            return servletRequest.getServletRequest().getParameter("token");
        }
        return null;
    }
}
