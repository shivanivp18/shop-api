
package com.shivani.shop_api.auth;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // BCrypt password hashing for registration and login
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // Disable CSRF for stateless REST APIs
            .csrf(csrf -> csrf.disable())

            // JWT authentication does not use HTTP sessions
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Return 401 for unauthenticated requests
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(
                    (request, response, authException) ->
                        response.sendError(
                            HttpStatus.UNAUTHORIZED.value(),
                            "Unauthorized"
                        )
                )
            )

            .authorizeHttpRequests(auth -> auth

                // Allow Spring Boot internal error dispatches
                .dispatcherTypeMatchers(
                    DispatcherType.ERROR
                ).permitAll()

                // Public registration and login
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/register",
                    "/api/auth/login"
                ).permitAll()

                // Only ADMIN can create products
                .requestMatchers(
                    HttpMethod.POST,
                    "/products",
                    "/products/**"
                ).hasRole("ADMIN")

                // Only ADMIN can update products
                .requestMatchers(
                    HttpMethod.PUT,
                    "/products",
                    "/products/**"
                ).hasRole("ADMIN")

                // Only ADMIN can delete products
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/products",
                    "/products/**"
                ).hasRole("ADMIN")

                // Public product browsing
                .requestMatchers(
                    HttpMethod.GET,
                    "/products",
                    "/products/**"
                ).permitAll()

                // ADMIN-only endpoints
                .requestMatchers(
                    "/api/admin/**"
                ).hasRole("ADMIN")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            // Validate JWT before username/password authentication
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
