
package com.shivani.shop_api.auth;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // JWT authentication filter
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Constructor injection
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // Password encoder used during registration and login
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Configure Spring Security
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // Disable CSRF for our stateless REST API

.csrf(csrf -> csrf.disable())

// Return HTTP 401 when authentication is missing
.exceptionHandling(exceptions -> exceptions
    .authenticationEntryPoint((request, response, authException) -> {
        response.sendError(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized"
        );
    })
)




            // Configure endpoint permissions
            .authorizeHttpRequests(auth -> auth

                // Allow Spring Boot error handling
                .dispatcherTypeMatchers(
                    DispatcherType.ERROR,
                    DispatcherType.FORWARD
                ).permitAll()

                // Allow registration and login without JWT
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/register",
                    "/api/auth/login"
                ).permitAll()

                // Only ADMIN users can access admin endpoints
.requestMatchers("/api/admin/**").hasRole("ADMIN")

// Anyone can browse products
.requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()

// All other endpoints require authentication
.anyRequest().authenticated()
            )

            // Use stateless JWT authentication
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // Run JWT filter before username/password authentication
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );


        return http.build();
    }
}
