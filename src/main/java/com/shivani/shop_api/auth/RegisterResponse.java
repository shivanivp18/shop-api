
package com.shivani.shop_api.auth;

import java.time.LocalDateTime;

public record RegisterResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        LocalDateTime createdAt
) {

    public static RegisterResponse fromUser(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
