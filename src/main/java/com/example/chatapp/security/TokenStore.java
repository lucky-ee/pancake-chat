package com.example.chatapp.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory bearer token store.
 *
 * Tokens are opaque random UUIDs mapped to a username, with a fixed TTL.
 * This is intentionally lightweight (no external session store, no JWT
 * signing/parsing) — fine for a single-instance app or local dev. For a
 * multi-instance deployment, swap this for JWTs or a shared store (Redis).
 */
@Component
public class TokenStore {

    private static final long TTL_MILLIS = 24 * 60 * 60 * 1000L; // 24 hours

    private record TokenInfo(String username, Instant expiresAt) {}

    private final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    public String issueToken(String username) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, new TokenInfo(username, Instant.now().plusMillis(TTL_MILLIS)));
        return token;
    }

    /** Returns the username for a valid, non-expired token; empty otherwise. */
    public Optional<String> validate(String token) {
        if (token == null) return Optional.empty();
        TokenInfo info = tokens.get(token);
        if (info == null) return Optional.empty();
        if (Instant.now().isAfter(info.expiresAt())) {
            tokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(info.username());
    }

    public void revoke(String token) {
        if (token != null) tokens.remove(token);
    }
}
