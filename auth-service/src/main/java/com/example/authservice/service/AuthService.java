package com.example.authservice.service;

import com.example.authservice.dto.LoginRequestDTO;
import com.example.authservice.model.User;

import com.example.authservice.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Authenticates a user based on the provided login credentials.
     *
     * @param loginRequestDTO - contains user's email and password
     * @return an Optional JWT token if authentication is successful, or an empty Optional if it fails
     */
    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {

        // Try to find a user by email from the database using the user service
        Optional<String> token = userService
                .findByEmail(loginRequestDTO.getEmail())

                // Filter to ensure the provided password matches the stored hashed password
                .filter(u -> passwordEncoder.matches(loginRequestDTO.getPassword(), u.getPassword()))

                // If password matches, generate a JWT token using the user's email and role
                .map(u -> jwtUtil.generateToken(u.getEmail(), u.getRole()));

        // Return the generated token, or empty if authentication fails
        return token;
    }


    public boolean validateToken(String token) {
        try {
            jwtUtil.validateToken(token);
            return true;

        } catch (JwtException e) {
            return false;
        }
    }
}
