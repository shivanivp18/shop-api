
package com.shivani.shop_api.order;

import com.shivani.shop_api.auth.User;
import com.shivani.shop_api.auth.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderHistoryService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderHistoryService(
            OrderRepository orderRepository,
            UserRepository userRepository) {

        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Order> getCustomerOrders(String customerEmail) {

        User customer = userRepository
                .findByEmail(customerEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found"
                        )
                );

        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(
                        customer.getId()
                );
    }

    @Transactional(readOnly = true)
    public Order getCustomerOrder(
            String customerEmail,
            Long orderId) {

        User customer = userRepository
                .findByEmail(customerEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found"
                        )
                );

        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND,
                                "Order not found"
                        )
                );

        // Prevent customers from accessing someone else's order
        if (!order.getUser().getId().equals(customer.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Order not found"
            );
        }

        return order;
    }

}
