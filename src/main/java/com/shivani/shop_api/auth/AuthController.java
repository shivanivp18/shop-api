
package com.shivani.shop_api.auth;

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
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        // Step 1: Register the user through AuthService
        User savedUser = authService.register(request);

        // Step 2: Convert the User entity to a safe DTO
        RegisterResponse response =
                RegisterResponse.fromUser(savedUser);

        // Step 3: Return HTTP 201 Created
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
