
package com.shivani.shop_api.cart;

import com.shivani.shop_api.auth.User;
import com.shivani.shop_api.auth.UserRepository;
import com.shivani.shop_api.model.Product;
import com.shivani.shop_api.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository) {

        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    // Find an existing cart, or create one for the customer
    @Transactional
    public Cart getOrCreateCart(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(new Cart(user)));
    }

    // Retrieve all products in the customer's cart
    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        return cartRepository.findByUserId(user.getId())
                .map(cart -> cartItemRepository.findByCartId(cart.getId()))
                .orElse(List.of());
    }

    // Add a product to the customer's cart
    @Transactional
    public CartItem addProduct(
            String email,
            Long productId,
            int quantity) {

        if (quantity <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be greater than zero"
            );
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        Cart cart = getOrCreateCart(email);

        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseGet(() -> new CartItem(cart, product, 0));

        long newQuantity = (long) item.getQuantity() + quantity;

        validateStock(product, newQuantity);

        item.setQuantity((int) newQuantity);

        return cartItemRepository.save(item);
    }

    // Change the quantity of an existing cart item
    @Transactional
    public CartItem updateQuantity(
            String email,
            Long productId,
            int quantity) {

        if (quantity <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be greater than zero"
            );
        }

        CartItem item = findOwnedCartItem(email, productId);

        validateStock(item.getProduct(), quantity);

        item.setQuantity(quantity);

        return cartItemRepository.save(item);
    }

    // Remove a product from the customer's cart
    @Transactional
    public void removeProduct(String email, Long productId) {

        CartItem item = findOwnedCartItem(email, productId);

        cartItemRepository.delete(item);
    }

    // Ensure the cart item belongs to the logged-in customer
    private CartItem findOwnedCartItem(
            String email,
            Long productId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cart not found"
                ));

        return cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found in cart"
                ));
    }

    // Prevent invalid quantities and requests exceeding stock
    private void validateStock(Product product, long quantity) {

        if (quantity > product.getStock()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Requested quantity exceeds available stock"
            );
        }
    }
}
