package com.example.chatapp.security;

import java.security.Principal;

/** Minimal Principal carrying the authenticated username through the WebSocket session. */
public class StompPrincipal implements Principal {

    private final String name;

    public StompPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
