
package com.shivani.shop_api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
  
public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService) {

    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
}


    public User register(RegisterRequest request) {

        // Step 1: Normalize email
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Step 2: Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        // Step 3: Create a new user
        User user = new User();

        user.setFullName(request.fullName().trim());
        user.setEmail(email);

        // Step 4: Hash password securely
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        // Step 5: Assign CUSTOMER role
        user.setRole(Role.CUSTOMER);

        // Step 6: Save user in MySQL
        return userRepository.save(user);
    }
}
