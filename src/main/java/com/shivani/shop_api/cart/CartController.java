
package com.shivani.shop_api.cart;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // View the logged-in customer's cart
    @GetMapping
    public List<CartItemResponse> getCart(
            Authentication authentication) {

        List<CartItem> items = cartService.getCartItems(
                authentication.getName()
        );

        return items.stream()
                .map(CartItemResponse::from)
                .toList();
    }

    // Add a product to the cart
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemResponse addProduct(
            Authentication authentication,
            @RequestBody AddCartItemRequest request) {

        CartItem item = cartService.addProduct(
                authentication.getName(),
                request.productId(),
                request.quantity()
        );

        return CartItemResponse.from(item);
    }

    // Update the quantity of a cart item
    @PutMapping("/items/{productId}")
    public CartItemResponse updateQuantity(
            Authentication authentication,
            @PathVariable Long productId,
            @RequestBody UpdateCartItemRequest request) {

        CartItem item = cartService.updateQuantity(
                authentication.getName(),
                productId,
                request.quantity()
        );

        return CartItemResponse.from(item);
    }

    // Remove a product from the cart
    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProduct(
            Authentication authentication,
            @PathVariable Long productId) {

        cartService.removeProduct(
                authentication.getName(),
                productId
        );
    }

    public record AddCartItemRequest(
            Long productId,
            int quantity
    ) {}

    public record UpdateCartItemRequest(
            int quantity
    ) {}

    public record CartItemResponse(
            Long productId,
            String productName,
            double price,
            int quantity,
            double subtotal
    ) {

        public static CartItemResponse from(CartItem item) {

            double price = item.getProduct().getPrice();
            int quantity = item.getQuantity();

            return new CartItemResponse(
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    price,
                    quantity,
                    price * quantity
            );
        }
    }
}
