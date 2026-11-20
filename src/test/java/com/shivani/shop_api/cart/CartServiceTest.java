
package com.shivani.shop_api.cart;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
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
import com.shivani.shop_api.model.Product;
import com.shivani.shop_api.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    // TEST 1: Return existing cart
    @Test
    void getOrCreateCart_shouldReturnExistingCart() {
        String email = "customer@example.com";
        User user = mock(User.class);
        Cart cart = mock(Cart.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1L);
        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        Cart result = cartService.getOrCreateCart(email);

        assertSame(cart, result);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    // TEST 2: Create cart if none exists
    @Test
    void getOrCreateCart_shouldCreateNewCart() {
        String email = "customer@example.com";
        User user = mock(User.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1L);
        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        Cart result = cartService.getOrCreateCart(email);

        assertNotNull(result);
        verify(cartRepository).save(any(Cart.class));
    }

    // TEST 3: Reject unknown user
    @Test
    void getOrCreateCart_shouldRejectUnknownUser() {
        String email = "unknown@example.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cartService.getOrCreateCart(email)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
        verifyNoInteractions(cartRepository);
    }

    // TEST 4: Return empty list when cart does not exist
    @Test
    void getCartItems_shouldReturnEmptyListWithoutCart() {
        String email = "customer@example.com";
        User user = mock(User.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1L);
        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        List<CartItem> result = cartService.getCartItems(email);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(cartItemRepository);
    }

    // TEST 5: Return cart items
    @Test
    void getCartItems_shouldReturnExistingItems() {
        String email = "customer@example.com";
        User user = mock(User.class);
        Cart cart = mock(Cart.class);
        CartItem item = mock(CartItem.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1L);
        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));
        when(cart.getId()).thenReturn(10L);
        when(cartItemRepository.findByCartId(10L))
                .thenReturn(List.of(item));

        List<CartItem> result = cartService.getCartItems(email);

        assertEquals(1, result.size());
        assertSame(item, result.get(0));
    }

    // TEST 6: Reject zero quantity
    @Test
    void addProduct_shouldRejectZeroQuantity() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cartService.addProduct(
                        "customer@example.com", 1L, 0)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
        verifyNoInteractions(productRepository);
    }

    // TEST 7: Reject unavailable product
    @Test
    void addProduct_shouldRejectMissingProduct() {
        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cartService.addProduct(
                        "customer@example.com", 99L, 1)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }

    // TEST 8: Reject quantity exceeding stock
    @Test
    void addProduct_shouldRejectInsufficientStock() {
        String email = "customer@example.com";

        User user = mock(User.class);
        Product product = mock(Product.class);
        Cart cart = mock(Cart.class);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));
        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(5L);
        when(cartRepository.findByUserId(5L))
                .thenReturn(Optional.of(cart));
        when(cart.getId()).thenReturn(10L);
        when(cartItemRepository.findByCartIdAndProductId(10L, 1L))
                .thenReturn(Optional.empty());
        when(product.getStock()).thenReturn(2);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cartService.addProduct(email, 1L, 5)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
        verify(cartItemRepository, never())
                .save(any(CartItem.class));
    }

    // TEST 9: Remove an existing cart product
    @Test
    void removeProduct_shouldDeleteOwnedCartItem() {
        String email = "customer@example.com";

        User user = mock(User.class);
        Cart cart = mock(Cart.class);
        CartItem item = mock(CartItem.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(5L);
        when(cartRepository.findByUserId(5L))
                .thenReturn(Optional.of(cart));
        when(cart.getId()).thenReturn(10L);
        when(cartItemRepository.findByCartIdAndProductId(10L, 1L))
                .thenReturn(Optional.of(item));

        cartService.removeProduct(email, 1L);

        verify(cartItemRepository).delete(item);
    }
}
