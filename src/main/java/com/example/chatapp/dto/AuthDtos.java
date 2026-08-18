package com.example.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public static class AuthRequest {
        @NotBlank
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank
        @Size(min = 4, max = 100)
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class AuthResponse {
        private Long userId;
        private String username;
        private String message;
        private String token;

        public AuthResponse(Long userId, String username, String message, String token) {
            this.userId = userId;
            this.username = username;
            this.message = message;
            this.token = token;
        }

        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getMessage() { return message; }
        public String getToken() { return token; }
    }
}
