
package com.shivani.shop_api.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }


@Override
protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain)
        throws ServletException, IOException {

    // Step 1: Read Authorization header
    String authHeader = request.getHeader("Authorization");

    // Step 2: If there is no Bearer token, continue normally
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }

    // Step 3: Extract JWT from Authorization header
    String token = authHeader.substring(7);

    try {
        // Step 4: Extract customer email and verify JWT signature
        String email = jwtService.extractEmail(token);

        // Step 5: Check that the customer exists in MySQL
        userRepository.findByEmail(email).ifPresent(user -> {

            // Step 6: Verify the JWT and avoid replacing existing authentication
            if (jwtService.isTokenValid(token, user.getEmail())
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Step 7: Convert CUSTOMER/ADMIN role into Spring Security authority
                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

                // Step 8: Create authenticated user information
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user.getEmail(),
                                null,
                                List.of(authority)
                        );

                // Step 9: Store authentication in Spring Security
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
                        }
        });

    } catch (Exception exception) {
        // Invalid or expired JWT: do not authenticate the request.
        // Spring Security will reject protected requests later.
        SecurityContextHolder.clearContext();
    }

    // Step 10: Continue processing the HTTP request
    filterChain.doFilter(request, response);
}
}
