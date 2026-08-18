package com.example.chatapp.service;

import com.example.chatapp.dto.AuthDtos.AuthRequest;
import com.example.chatapp.exception.ApiExceptions.ConflictException;
import com.example.chatapp.exception.ApiExceptions.UnauthorizedException;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.security.TokenStore;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TokenStore tokenStore;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, TokenStore tokenStore) {
        this.userRepository = userRepository;
        this.tokenStore = tokenStore;
    }

    /** Issues a fresh bearer token for an already-authenticated user. */
    public String issueToken(User user) {
        return tokenStore.issueToken(user.getUsername());
    }

    public void logout(String token) {
        tokenStore.revoke(token);
    }

    public User register(AuthRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }
        User user = new User(request.getUsername(), passwordEncoder.encode(request.getPassword()));
        return userRepository.save(user);
    }

    public User login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }
        return user;
    }
}
