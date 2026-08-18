package com.example.chatapp.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

/**
 * Requires a valid "Authorization: Bearer <token>" header on protected REST
 * routes. On success, the resolved username is stashed as a request
 * attribute ("authUsername") for controllers/services to use if needed.
 */
@Component
public class TokenAuthInterceptor implements HandlerInterceptor {

    private final TokenStore tokenStore;

    public TokenAuthInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String header = request.getHeader("Authorization");
        String token = (header != null && header.startsWith("Bearer "))
                ? header.substring("Bearer ".length())
                : null;

        Optional<String> username = tokenStore.validate(token);
        if (username.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Missing or invalid bearer token\"}");
            return false;
        }

        request.setAttribute("authUsername", username.get());
        return true;
    }
}
