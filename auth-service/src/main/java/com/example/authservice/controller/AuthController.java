package com.example.authservice.controller;

import com.example.authservice.dto.LoginRequestDTO;
import com.example.authservice.dto.LoginResponseDTO;
import com.example.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @Operation(summary = "Generate token on user login")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
             @RequestBody LoginRequestDTO loginRequestDTO) {
        Optional<String> tokenOptional = authService.authenticate(loginRequestDTO);
        if (tokenOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }


        String token = tokenOptional.get();
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }



    @Operation(summary = "validate token")
// This Swagger/OpenAPI annotation provides documentation for the endpoint.
// summary: A short description of the API's function.

    @GetMapping("/validate")
// Maps HTTP GET requests to the "/validate" path to this method.
    public ResponseEntity<LoginResponseDTO> validateToken(
            // Extracts the value of the "Authorization" header from the incoming request.
            @RequestHeader("Authorization") String authHeader) {

        // Authorization: Bearer <token>

        // --- Initial Validation of Header Format ---

        // Check if the header is null or doesn't start with the required "Bearer " scheme.
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            // If the format is incorrect, return a 401 Unauthorized status with no body.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // --- Token Validation ---

        // Call the service layer to validate the token.
        // authHeader.substring(7) extracts the token by skipping the "Bearer " prefix (7 characters).
        return authService.validateToken(authHeader.substring(7))

                // Ternary operator for response:
                // IF validation succeeds (true): return 200 OK.
                ?   ResponseEntity.ok().build()

                // ELSE validation fails (false - e.g., expired or invalid signature): return 401 Unauthorized.
                :   ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

}
