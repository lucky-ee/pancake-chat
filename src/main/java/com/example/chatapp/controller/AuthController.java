package com.example.chatapp.controller;

import com.example.chatapp.dto.AuthDtos.AuthRequest;
import com.example.chatapp.dto.AuthDtos.AuthResponse;
import com.example.chatapp.model.User;
import com.example.chatapp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        User user = authService.register(request);
        String token = authService.issueToken(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(user.getId(), user.getUsername(), "Registered successfully", token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        User user = authService.login(request);
        String token = authService.issueToken(user);
        return ResponseEntity.ok(new AuthResponse(user.getId(), user.getUsername(), "Login successful", token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring("Bearer ".length()));
        }
        return ResponseEntity.noContent().build();
    }
}
