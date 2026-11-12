
package com.shivani.shop_api.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            // Disable CSRF for our stateless REST API
            .csrf(csrf -> csrf.disable())

            // Configure endpoint permissions
            .authorizeHttpRequests(auth -> auth

                // Anyone can register without logging in
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/register"
                ).permitAll()

                // All other requests require authentication
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
