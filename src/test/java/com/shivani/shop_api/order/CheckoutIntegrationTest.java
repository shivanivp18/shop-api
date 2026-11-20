
package com.shivani.shop_api.order;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.shivani.shop_api.auth.User;
import com.shivani.shop_api.auth.UserRepository;
import com.shivani.shop_api.cart.Cart;
import com.shivani.shop_api.cart.CartItem;
import com.shivani.shop_api.cart.CartItemRepository;
import com.shivani.shop_api.cart.CartRepository;
import com.shivani.shop_api.model.Product;
import com.shivani.shop_api.repository.ProductRepository;

@SpringBootTest
@ActiveProfiles("test")
class CheckoutIntegrationTest {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoSpyBean
    private OrderItemRepository orderItemRepository;

    private Long testUserId;
    private Long testProductId;

    // ----------------------------------------------------
    // HELPER METHODS
    // ----------------------------------------------------

    private User createCustomer() {

        User customer = new User();
        customer.setEmail(
                "checkout-" + UUID.randomUUID() + "@example.com"
        );
        customer.setFullName("Checkout Test Customer");
        customer.setPasswordHash("test-only-password-hash");

        customer = userRepository.save(customer);
        testUserId = customer.getId();

        return customer;
    }

    private Product createProduct(int stock) {

        Product product = new Product();
        product.setName("Integration Test Laptop");
        product.setDescription("Product for checkout testing");
        product.setCategory("Electronics");
        product.setPrice(50000);
        product.setStock(stock);

        product = productRepository.save(product);
        testProductId = product.getId();

        return product;
    }

    private Cart createCart(User customer) {
        return cartRepository.save(new Cart(customer));
    }

    private void addCartItem(
            Cart cart,
            Product product,
            int quantity) {

        cartItemRepository.save(
                new CartItem(cart, product, quantity)
        );
    }

    private List<Order> findCustomerOrders(Long userId) {

        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }

    private void assertProductStock(
            Long productId,
            int expectedStock) {

        Product product = productRepository
                .findById(productId)
                .orElseThrow();

        assertEquals(expectedStock, product.getStock());
    }

    private void assertCartQuantity(
            Long cartId,
            int expectedQuantity) {

        List<CartItem> items =
                cartItemRepository.findByCartId(cartId);

        assertEquals(1, items.size());
        assertEquals(
                expectedQuantity,
                items.get(0).getQuantity()
        );
    }

    // ----------------------------------------------------
    // CLEANUP AFTER EACH TEST
    // ----------------------------------------------------

    @AfterEach
    void cleanUp() {

        if (testUserId != null) {

            List<Order> orders =
                    findCustomerOrders(testUserId);

            for (Order order : orders) {

                orderItemRepository.deleteAll(
                        orderItemRepository.findByOrderId(
                                order.getId()
                        )
                );

                orderRepository.delete(order);
            }

            cartRepository.findByUserId(testUserId)
                    .ifPresent(cart -> {

                        cartItemRepository.deleteAll(
                                cartItemRepository.findByCartId(
                                        cart.getId()
                                )
                        );

                        cartRepository.delete(cart);
                    });
        }

        if (testProductId != null) {
            productRepository.deleteById(testProductId);
        }

        if (testUserId != null) {
            userRepository.deleteById(testUserId);
        }
    }

    // ----------------------------------------------------
    // TEST 1: SUCCESSFUL CHECKOUT
    // ----------------------------------------------------

    @Test
    void checkout_shouldCreateOrderAndUpdateInventoryInDatabase() {

        User customer = createCustomer();
        Product product = createProduct(10);
        Cart cart = createCart(customer);

        addCartItem(cart, product, 2);

        checkoutService.checkout(customer.getEmail());

        // Verify order was created
        List<Order> orders =
                findCustomerOrders(customer.getId());

        assertEquals(1, orders.size());

        Order order = orders.get(0);
        assertEquals(OrderStatus.PLACED, order.getStatus());

        // Verify order item details
        List<OrderItem> orderItems =
                orderItemRepository.findByOrderId(
                        order.getId()
                );

        assertEquals(1, orderItems.size());
        assertEquals(
                2,
                orderItems.get(0).getQuantity()
        );
        assertEquals(
                50000.0,
                orderItems.get(0).getPriceAtPurchase()
        );

        // Stock decreases from 10 to 8
        assertProductStock(product.getId(), 8);

        // Cart becomes empty
        assertTrue(
                cartItemRepository
                        .findByCartId(cart.getId())
                        .isEmpty()
        );
    }

    // ----------------------------------------------------
    // TEST 2: INSUFFICIENT STOCK
    // ----------------------------------------------------

    @Test
    void checkout_shouldNotChangeDatabaseWhenStockIsInsufficient() {

        User customer = createCustomer();
        Product product = createProduct(2);
        Cart cart = createCart(customer);

        // Customer requests 5, but only 2 are available
        addCartItem(cart, product, 5);

        String email = customer.getEmail();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> checkoutService.checkout(email)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Insufficient stock")
        );

        // No order created
        assertTrue(
                findCustomerOrders(customer.getId())
                        .isEmpty()
        );

        // Stock unchanged
        assertProductStock(product.getId(), 2);

        // Cart preserved
        assertCartQuantity(cart.getId(), 5);
    }

    // ----------------------------------------------------
    // TEST 3: INVALID QUANTITY
    // ----------------------------------------------------

    @Test
    void checkout_shouldRejectInvalidQuantity() {

        User customer = createCustomer();
        Product product = createProduct(10);
        Cart cart = createCart(customer);

        // Deliberately insert invalid quantity
        addCartItem(cart, product, 0);

        String email = customer.getEmail();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> checkoutService.checkout(email)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Invalid quantity")
        );

        // No order created
        assertTrue(
                findCustomerOrders(customer.getId())
                        .isEmpty()
        );

        // Stock remains unchanged
        assertProductStock(product.getId(), 10);

        // Invalid cart item remains for correction
        assertCartQuantity(cart.getId(), 0);
    }

    // ----------------------------------------------------
    // TEST 4: EMPTY CART
    // ----------------------------------------------------

    @Test
    void checkout_shouldRejectEmptyCart() {

        User customer = createCustomer();
        Cart cart = createCart(customer);

        String email = customer.getEmail();

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> checkoutService.checkout(email)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Cannot checkout an empty cart")
        );

        // No order should be created
        assertTrue(
                findCustomerOrders(customer.getId())
                        .isEmpty()
        );

        // Cart should stay empty
        assertTrue(
                cartItemRepository
                        .findByCartId(cart.getId())
                        .isEmpty()
        );
    }

    // ----------------------------------------------------
    // TEST 5: REAL TRANSACTION ROLLBACK
    // ----------------------------------------------------

    @Test
    void checkout_shouldRollbackWhenOrderItemSaveFails() {

        User customer = createCustomer();
        Product product = createProduct(10);
        Cart cart = createCart(customer);

        addCartItem(cart, product, 3);

        // Simulate a database-layer failure AFTER
        // CheckoutService saves the order.
        doThrow(
                new IllegalStateException(
                        "Simulated order item failure"
                )
        ).when(orderItemRepository)
                .save(any(OrderItem.class));

        String email = customer.getEmail();

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> checkoutService.checkout(email)
                );

        assertEquals(
                "Simulated order item failure",
                exception.getMessage()
        );

        // The earlier order insert must be rolled back
        assertTrue(
                findCustomerOrders(customer.getId())
                        .isEmpty()
        );

        // Product inventory must remain unchanged
        assertProductStock(product.getId(), 10);

        // The customer's cart must remain unchanged
        assertCartQuantity(cart.getId(), 3);
    }
}
