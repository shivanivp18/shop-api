
package com.shivani.shop_api.order;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shivani.shop_api.auth.User;
import com.shivani.shop_api.auth.UserRepository;
import com.shivani.shop_api.cart.CartItem;
import com.shivani.shop_api.cart.CartService;
import com.shivani.shop_api.model.Product;
import com.shivani.shop_api.repository.ProductRepository;

@Service
public class CheckoutService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;

    public CheckoutService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository,
            CartService cartService,
            ProductRepository productRepository) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.productRepository = productRepository;
    }

    @Transactional
    public void checkout(String customerEmail) {

        // Step 1: Identify the customer
        User customer = userRepository
                .findByEmail(customerEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found"
                        )
                );

        // Step 2: Retrieve shopping cart items
        List<CartItem> cartItems =
                cartService.getCartItems(customerEmail);

        // Step 3: Reject an empty cart
        
            if (cartItems.isEmpty()) {
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Cannot checkout an empty cart"
    );
}

        // Step 4: Sort product IDs to reduce deadlock risk
        List<CartItem> sortedItems = cartItems.stream()
                .sorted(Comparator.comparing(
                        item -> item.getProduct().getId()
                ))
                .toList();

        // Step 5: Lock products and validate stock
        List<Product> lockedProducts = new ArrayList<>();

        for (CartItem item : sortedItems) {

            Long productId = item.getProduct().getId();

            Product product = productRepository
                    .findByIdForUpdate(productId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Product not found: " + productId
                            )
                    );

            int quantity = item.getQuantity();

            if (quantity <= 0) {
                throw new IllegalArgumentException(
                        "Invalid quantity for product: "
                        + product.getName()
                );
            }

            if (quantity > product.getStock()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: "
                        + product.getName()
                );
            }

            lockedProducts.add(product);
        }

        // Step 6: Create and save the order
        Order order = new Order(customer);
        Order savedOrder = orderRepository.save(order);

        // Step 7: Save order items and deduct inventory
        for (int i = 0; i < sortedItems.size(); i++) {

            CartItem cartItem = sortedItems.get(i);
            Product product = lockedProducts.get(i);

            int quantity = cartItem.getQuantity();

            // Save product details at purchase time
            OrderItem orderItem = new OrderItem(
                    savedOrder,
                    product,
                    quantity
            );

            orderItemRepository.save(orderItem);

            // Deduct purchased quantity from stock
            int newStock = product.getStock() - quantity;
            product.setStock(newStock);

            // Persist updated inventory
            productRepository.save(product);
        }

       
        // Step 8: Remove purchased products from the cart
        for (CartItem item : sortedItems) {
            cartService.removeProduct(
                    customerEmail,
                    item.getProduct().getId()
            );
        }

    }
}
