package com.example.chatapp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses a real (in-memory) TokenStore rather than a mock: it's a trivial,
 * deterministic collaborator, so exercising the real validate()/issueToken()
 * logic gives more genuine coverage than stubbing it out would.
 */
class AuthHandshakeInterceptorTest {

    private TokenStore tokenStore;
    private AuthHandshakeInterceptor interceptor;
    private MockHttpServletResponse mockResponse;
    private ServerHttpResponse response;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        tokenStore = new TokenStore();
        interceptor = new AuthHandshakeInterceptor(tokenStore);
        mockResponse = new MockHttpServletResponse();
        response = new ServletServerHttpResponse(mockResponse);
        attributes = new HashMap<>();
    }

    private ServerHttpRequest requestWithParams(Map<String, String> params) {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        params.forEach(mockRequest::setParameter);
        return new ServletServerHttpRequest(mockRequest);
    }

    @Test
    void validToken_bindsCorrectUsernameAndAllowsHandshake() {
        String token = tokenStore.issueToken("alice");
        ServerHttpRequest request = requestWithParams(Map.of("token", token));

        boolean allowed = interceptor.beforeHandshake(request, response, null, attributes);

        assertThat(allowed).isTrue();
        assertThat(attributes).containsEntry("username", "alice");
    }

    @Test
    void missingToken_rejectsHandshakeWith401AndSetsNoAttributes() {
        ServerHttpRequest request = requestWithParams(Map.of());

        boolean allowed = interceptor.beforeHandshake(request, response, null, attributes);

        assertThat(allowed).isFalse();
        assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(attributes).isEmpty();
    }

    @Test
    void invalidToken_rejectsHandshakeWith401AndSetsNoAttributes() {
        // A well-formed but never-issued token — the "invalid" case, distinct
        // from "missing" above.
        ServerHttpRequest request = requestWithParams(Map.of("token", "not-a-real-token"));

        boolean allowed = interceptor.beforeHandshake(request, response, null, attributes);

        assertThat(allowed).isFalse();
        assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(attributes).isEmpty();
    }

    @Test
    void spoofedUsernameParam_isIgnored_identityComesOnlyFromTheValidatedToken() {
        String token = tokenStore.issueToken("alice");
        // Attacker rides along a legitimately-issued token but also injects a
        // "username" query param, hoping something downstream trusts it.
        ServerHttpRequest request = requestWithParams(Map.of("token", token, "username", "eve"));

        interceptor.beforeHandshake(request, response, null, attributes);

        assertThat(attributes.get("username")).isEqualTo("alice");
    }
}
