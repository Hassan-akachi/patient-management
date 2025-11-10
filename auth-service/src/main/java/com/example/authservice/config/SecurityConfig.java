package com.example.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
// Define a Bean that configures the security filter chain for the application.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // Configure authorization rules: allow all requests to be accessed without authentication.
        http.authorizeHttpRequests(auhtorize -> auhtorize.anyRequest().permitAll())

                // Disable Cross-Site Request Forgery (CSRF) protection.
                // This is common for state-less APIs (like REST).
                .csrf(AbstractHttpConfigurer ::disable);

        // Build and return the configured SecurityFilterChain.
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
