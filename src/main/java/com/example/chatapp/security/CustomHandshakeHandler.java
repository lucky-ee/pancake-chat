package com.example.chatapp.security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Runs right after AuthHandshakeInterceptor approves the handshake.
 * Reads the "username" attribute it stashed and binds it as this WebSocket
 * session's Principal. From here on, every STOMP frame on this connection
 * (including @MessageMapping handler calls) can trust Principal#getName()
 * as the real, server-verified identity — never something the client claims
 * in a message payload.
 */
@Component
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
        Object username = attributes.get("username");
        if (username == null) {
            // Should never happen — AuthHandshakeInterceptor rejects the handshake
            // before we get here if there's no valid, authenticated username.
            throw new IllegalStateException("WebSocket handshake reached without an authenticated username");
        }
        return new StompPrincipal(username.toString());
    }
}
