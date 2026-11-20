
package com.shivani.shop_api.order;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
class ConcurrentCheckoutIntegrationTest {

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

    @Autowired
    private OrderItemRepository orderItemRepository;

    // Track only records created by this test
    private final List<Long> testUserIds = new ArrayList<>();

    private Long testProductId;

    // ----------------------------------------------------
    // HELPER 1: CREATE TEST CUSTOMER
    // ----------------------------------------------------

    private User createCustomer(String name) {

        User customer = new User();

        customer.setEmail(
                "concurrent-" +
                UUID.randomUUID() +
                "@example.com"
        );

        customer.setFullName(name);

        customer.setPasswordHash(
                "test-only-password-hash"
        );

        customer = userRepository.save(customer);

        testUserIds.add(customer.getId());

        return customer;
    }

    // ----------------------------------------------------
    // HELPER 2: CREATE TEST PRODUCT
    // ----------------------------------------------------

    private Product createProduct(int stock) {

        Product product = new Product();

        product.setName(
                "Concurrent Checkout Test Laptop"
        );

        product.setDescription(
                "Product for testing simultaneous checkout"
        );

        product.setCategory("Electronics");

        product.setPrice(50000);

        product.setStock(stock);

        product = productRepository.save(product);

        testProductId = product.getId();

        return product;
    }

    // ----------------------------------------------------
    // HELPER 3: CREATE CUSTOMER CART
    // ----------------------------------------------------

    private Cart createCartWithProduct(
            User customer,
            Product product,
            int quantity) {

        Cart cart = cartRepository.save(
                new Cart(customer)
        );

        cartItemRepository.save(
                new CartItem(
                        cart,
                        product,
                        quantity
                )
        );

        return cart;
    }

    // ----------------------------------------------------
    // CLEANUP TEST RECORDS
    // ----------------------------------------------------

    @AfterEach
    void cleanUp() {

        for (Long userId : testUserIds) {

            // First remove order items and orders
            List<Order> orders =
                    orderRepository
                            .findByUserIdOrderByCreatedAtDesc(
                                    userId
                            );

            for (Order order : orders) {

                orderItemRepository.deleteAll(
                        orderItemRepository.findByOrderId(
                                order.getId()
                        )
                );

                orderRepository.delete(order);
            }

            // Then remove cart items and cart
            cartRepository.findByUserId(userId)
                    .ifPresent(cart -> {

                        cartItemRepository.deleteAll(
                                cartItemRepository.findByCartId(
                                        cart.getId()
                                )
                        );

                        cartRepository.delete(cart);
                    });
        }

        // Remove test product
        if (testProductId != null) {
            productRepository.deleteById(
                    testProductId
            );
        }

        // Remove test customers
        for (Long userId : testUserIds) {
            userRepository.deleteById(userId);
        }
    }

    // ----------------------------------------------------
    // MAIN TEST: TWO CUSTOMERS CHECKOUT SIMULTANEOUSLY
    // ----------------------------------------------------

    @Test
    void concurrentCheckout_shouldPreventOverselling()
            throws Exception {

        // STEP 1: Create two different customers
        User customerA = createCustomer(
                "Concurrent Customer A"
        );

        User customerB = createCustomer(
                "Concurrent Customer B"
        );

        // STEP 2: Product has only 5 units
        Product product = createProduct(5);

        // STEP 3: Both customers request 4 units
        Cart cartA = createCartWithProduct(
                customerA,
                product,
                4
        );

        Cart cartB = createCartWithProduct(
                customerB,
                product,
                4
        );

        String emailA = customerA.getEmail();
        String emailB = customerB.getEmail();

        // STEP 4: Create two worker threads
        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        // Wait until both threads are ready
        CountDownLatch readyLatch =
                new CountDownLatch(2);

        // Release both threads together
        CountDownLatch startLatch =
                new CountDownLatch(1);

        // STEP 5: Define a checkout task
        Callable<String> checkoutTaskA = () -> {

            readyLatch.countDown();

            if (!startLatch.await(
                    10,
                    TimeUnit.SECONDS)) {

                throw new IllegalStateException(
                        "Timed out waiting to start"
                );
            }

            try {

                checkoutService.checkout(emailA);

                return "SUCCESS";

            } catch (IllegalArgumentException ex) {

                if (ex.getMessage() != null
                        && ex.getMessage().contains(
                                "Insufficient stock")) {

                    return "INSUFFICIENT_STOCK";
                }

                throw ex;
            }
        };

        Callable<String> checkoutTaskB = () -> {

            readyLatch.countDown();

            if (!startLatch.await(
                    10,
                    TimeUnit.SECONDS)) {

                throw new IllegalStateException(
                        "Timed out waiting to start"
                );
            }

            try {

                checkoutService.checkout(emailB);

                return "SUCCESS";

            } catch (IllegalArgumentException ex) {

                if (ex.getMessage() != null
                        && ex.getMessage().contains(
                                "Insufficient stock")) {

                    return "INSUFFICIENT_STOCK";
                }

                throw ex;
            }
        };

        // STEP 6: Submit both checkout tasks
        Future<String> futureA = null;
        Future<String> futureB = null;

        String resultA;
        String resultB;

        try {

            futureA = executor.submit(checkoutTaskA);

            futureB = executor.submit(checkoutTaskB);

            // Ensure both workers are ready
            assertTrue(
                    readyLatch.await(
                            10,
                            TimeUnit.SECONDS
                    ),
                    "Both checkout threads must be ready"
            );

            // Start both checkouts together
            startLatch.countDown();

            // STEP 7: Wait for both results
            resultA = futureA.get(
                    30,
                    TimeUnit.SECONDS
            );

            resultB = futureB.get(
                    30,
                    TimeUnit.SECONDS
            );

        } finally {

            startLatch.countDown();

            if (futureA != null) {
                futureA.cancel(true);
            }

            if (futureB != null) {
                futureB.cancel(true);
            }

            executor.shutdown();

            assertTrue(
                    executor.awaitTermination(
                            30,
                            TimeUnit.SECONDS
                    ),
                    "Checkout worker threads did not finish"
            );
        }

        // STEP 8: Count successful checkouts
        long successfulCheckouts =
                List.of(resultA, resultB)
                        .stream()
                        .filter(
                                "SUCCESS"::equals
                        )
                        .count();

        long rejectedCheckouts =
                List.of(resultA, resultB)
                        .stream()
                        .filter(
                                "INSUFFICIENT_STOCK"::equals
                        )
                        .count();

        assertEquals(
                1,
                successfulCheckouts,
                "Exactly one checkout must succeed"
        );

        assertEquals(
                1,
                rejectedCheckouts,
                "Exactly one checkout must fail due to stock"
        );

        // STEP 9: Verify final stock is 1
        Product updatedProduct =
                productRepository
                        .findById(product.getId())
                        .orElseThrow();

        assertEquals(
                1,
                updatedProduct.getStock(),
                "Inventory must not be oversold"
        );

        // STEP 10: Verify exactly one order exists
        List<Order> ordersA =
                orderRepository
                        .findByUserIdOrderByCreatedAtDesc(
                                customerA.getId()
                        );

        List<Order> ordersB =
                orderRepository
                        .findByUserIdOrderByCreatedAtDesc(
                                customerB.getId()
                        );

        assertEquals(
                1,
                ordersA.size() + ordersB.size(),
                "Only one order must be created"
        );

        // STEP 11: Verify winning customer's
        // cart is empty and losing customer's
        // cart still contains 4 units

        if ("SUCCESS".equals(resultA)) {

            assertTrue(
                    cartItemRepository
                            .findByCartId(cartA.getId())
                            .isEmpty()
            );

            List<CartItem> remainingItems =
                    cartItemRepository
                            .findByCartId(cartB.getId());

            assertEquals(1, remainingItems.size());

            assertEquals(
                    4,
                    remainingItems.get(0).getQuantity()
            );

        } else {

            assertTrue(
                    cartItemRepository
                            .findByCartId(cartB.getId())
                            .isEmpty()
            );

            List<CartItem> remainingItems =
                    cartItemRepository
                            .findByCartId(cartA.getId());

            assertEquals(1, remainingItems.size());

            assertEquals(
                    4,
                    remainingItems.get(0).getQuantity()
            );
        }

        // STEP 12: Print test results
        System.out.println(
                "Customer A result: " + resultA
        );

        System.out.println(
                "Customer B result: " + resultB
        );

        System.out.println(
                "Final product stock: " +
                updatedProduct.getStock()
        );

        System.out.println(
                "Concurrent checkout test passed."
        );
    }
}
