
package com.shivani.shop_api.order;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.shivani.shop_api.auth.User;
import com.shivani.shop_api.auth.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrderHistoryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderHistoryService orderHistoryService;

    private final String email = "customer@example.com";

    // TEST 1: Return customer order history
    @Test
    void getCustomerOrders_shouldReturnOrders() {
        User customer = mock(User.class);
        Order order1 = mock(Order.class);
        Order order2 = mock(Order.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(customer));
        when(customer.getId()).thenReturn(1L);
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(order1, order2));

        List<Order> result =
                orderHistoryService.getCustomerOrders(email);

        assertEquals(2, result.size());
        verify(orderRepository)
                .findByUserIdOrderByCreatedAtDesc(1L);
    }

    // TEST 2: Return empty history
    @Test
    void getCustomerOrders_shouldReturnEmptyList() {
        User customer = mock(User.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(customer));
        when(customer.getId()).thenReturn(1L);
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        List<Order> result =
                orderHistoryService.getCustomerOrders(email);

        assertTrue(result.isEmpty());
    }

    // TEST 3: Return an order belonging to customer
    @Test
    void getCustomerOrder_shouldReturnOwnedOrder() {
        User customer = mock(User.class);
        Order order = mock(Order.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(customer));
        when(customer.getId()).thenReturn(1L);
        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));
        when(order.getUser()).thenReturn(customer);

        Order result =
                orderHistoryService.getCustomerOrder(email, 10L);

        assertSame(order, result);
    }

    // TEST 4: Reject nonexistent order
    @Test
    void getCustomerOrder_shouldThrow404WhenMissing() {
        User customer = mock(User.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(customer));
        when(orderRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderHistoryService.getCustomerOrder(email, 99L)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }

    // TEST 5: Prevent access to another customer's order
    @Test
    void getCustomerOrder_shouldRejectOtherCustomersOrder() {
        User customer = mock(User.class);
        User otherCustomer = mock(User.class);
        Order order = mock(Order.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(customer));
        when(customer.getId()).thenReturn(1L);
        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(order));
        when(order.getUser()).thenReturn(otherCustomer);
        when(otherCustomer.getId()).thenReturn(2L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderHistoryService.getCustomerOrder(email, 10L)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }
}
