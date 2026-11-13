
package com.shivani.shop_api.auth;

public record LoginResponse(

        String accessToken,

        String tokenType,

        long expiresIn

) {
}
