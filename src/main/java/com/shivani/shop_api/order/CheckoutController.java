
package com.shivani.shop_api.order;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String checkout(Authentication authentication) {

        String customerEmail = authentication.getName();

        checkoutService.checkout(customerEmail);

        return "Order placed successfully";
    }
}
