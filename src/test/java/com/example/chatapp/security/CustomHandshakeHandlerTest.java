package com.example.chatapp.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * This is the class that actually mints the session's Principal, so it's the
 * direct target for the "can the identity be spoofed" question: does it ever
 * read identity from anything the client controls, or strictly from the
 * "username" attribute AuthHandshakeInterceptor already validated?
 *
 * determineUser() is protected on DefaultHandshakeHandler; this test lives in
 * the same package so it can call it directly without reflection.
 */
class CustomHandshakeHandlerTest {

    private final CustomHandshakeHandler handler = new CustomHandshakeHandler();

    private ServerHttpRequest plainRequest() {
        return new ServletServerHttpRequest(new MockHttpServletRequest());
    }

    @Test
    void determineUser_withUsernameAttribute_returnsMatchingStompPrincipal() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("username", "alice");

        Principal principal = handler.determineUser(plainRequest(), null, attributes);

        assertThat(principal).isInstanceOf(StompPrincipal.class);
        assertThat(principal.getName()).isEqualTo("alice");
    }

    @Test
    void determineUser_withoutUsernameAttribute_failsClosedInsteadOfAllowingAnonymous() {
        Map<String, Object> attributes = new HashMap<>();

        assertThatThrownBy(() -> handler.determineUser(plainRequest(), null, attributes))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void determineUser_ignoresClientSuppliedIdentityOnTheRequest_bindsOnlyTheServerValidatedAttribute() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        // Attacker-controlled data placed directly on the handshake request —
        // separate from the "username" attribute AuthHandshakeInterceptor set
        // server-side after validating the real token.
        mockRequest.setParameter("username", "eve");
        mockRequest.addHeader("X-Username", "eve");
        ServerHttpRequest request = new ServletServerHttpRequest(mockRequest);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("username", "alice");

        Principal principal = handler.determineUser(request, null, attributes);

        assertThat(principal.getName()).isEqualTo("alice");
    }
}
