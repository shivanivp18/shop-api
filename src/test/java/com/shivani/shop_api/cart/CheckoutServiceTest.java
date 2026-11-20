
package com.shivani.shop_api.order;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.shivani.shop_api.auth.User;
import com.shivani.shop_api.auth.UserRepository;
import com.shivani.shop_api.cart.CartItem;
import com.shivani.shop_api.cart.CartService;
import com.shivani.shop_api.model.Product;
import com.shivani.shop_api.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartService cartService;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CheckoutService checkoutService;

    private final String email = "customer@example.com";

    // TEST 1: Reject missing customer
    @Test
    void checkout_shouldRejectMissingCustomer() {
        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> checkoutService.checkout(email)
        );

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(cartService);
    }

    // TEST 2: Reject empty cart
    @Test
    void checkout_shouldRejectEmptyCart() {
        User user = mock(User.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(cartService.getCartItems(email))
                .thenReturn(List.of());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> checkoutService.checkout(email)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
        verifyNoInteractions(orderRepository);
    }

    // TEST 3: Reject missing product during checkout
    @Test
    void checkout_shouldRejectMissingProduct() {
        User user = mock(User.class);
        Product product = mock(Product.class);
        CartItem item = mock(CartItem.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(cartService.getCartItems(email))
                .thenReturn(List.of(item));
        when(item.getProduct()).thenReturn(product);
        when(product.getId()).thenReturn(1L);
        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> checkoutService.checkout(email)
        );

        verifyNoInteractions(orderRepository);
    }

    // TEST 4: Reject zero quantity
    @Test
    void checkout_shouldRejectZeroQuantity() {
        User user = mock(User.class);
        Product product = mock(Product.class);
        CartItem item = mock(CartItem.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(cartService.getCartItems(email))
                .thenReturn(List.of(item));
        when(item.getProduct()).thenReturn(product);
        when(product.getId()).thenReturn(1L);
        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                IllegalArgumentException.class,
                () -> checkoutService.checkout(email)
        );

        verifyNoInteractions(orderRepository);
    }

    // TEST 5: Reject insufficient stock
    @Test
    void checkout_shouldRejectInsufficientStock() {
        User user = mock(User.class);
        Product product = mock(Product.class);
        CartItem item = mock(CartItem.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(cartService.getCartItems(email))
                .thenReturn(List.of(item));
        when(item.getProduct()).thenReturn(product);
        when(product.getId()).thenReturn(1L);
        when(item.getQuantity()).thenReturn(5);
        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));
        when(product.getStock()).thenReturn(2);

        assertThrows(
                IllegalArgumentException.class,
                () -> checkoutService.checkout(email)
        );

        verifyNoInteractions(orderRepository);
        verify(productRepository, never())
                .save(any(Product.class));
    }

    // TEST 6: Successful checkout saves order and inventory
    @Test
    void checkout_shouldCreateOrderAndReduceStock() {
        User user = mock(User.class);
        Product product = mock(Product.class);
        CartItem item = mock(CartItem.class);
        Order savedOrder = mock(Order.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(cartService.getCartItems(email))
                .thenReturn(List.of(item));
        when(item.getProduct()).thenReturn(product);
        when(product.getId()).thenReturn(1L);
        when(item.getQuantity()).thenReturn(2);
        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));
        when(product.getStock()).thenReturn(10);
        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        checkoutService.checkout(email);

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository)
                .save(any(OrderItem.class));
        verify(product).setStock(8);
        verify(productRepository).save(product);
        verify(cartService).removeProduct(email, 1L);
    }

    // TEST 7: Reject negative quantity
    @Test
    void checkout_shouldRejectNegativeQuantity() {
        User user = mock(User.class);
        Product product = mock(Product.class);
        CartItem item = mock(CartItem.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(cartService.getCartItems(email))
                .thenReturn(List.of(item));
        when(item.getProduct()).thenReturn(product);
        when(product.getId()).thenReturn(1L);
        when(item.getQuantity()).thenReturn(-2);
        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                IllegalArgumentException.class,
                () -> checkoutService.checkout(email)
        );

        verifyNoInteractions(orderRepository);
    }

    // TEST 8: Do not clear cart when stock validation fails
    @Test
    void checkout_shouldPreserveCartOnStockFailure() {
        User user = mock(User.class);
        Product product = mock(Product.class);
        CartItem item = mock(CartItem.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(cartService.getCartItems(email))
                .thenReturn(List.of(item));
        when(item.getProduct()).thenReturn(product);
        when(product.getId()).thenReturn(1L);
        when(item.getQuantity()).thenReturn(10);
        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));
        when(product.getStock()).thenReturn(3);

        assertThrows(
                IllegalArgumentException.class,
                () -> checkoutService.checkout(email)
        );

        verify(cartService, never())
                .removeProduct(anyString(), anyLong());
        verifyNoInteractions(orderRepository);
    }
}
