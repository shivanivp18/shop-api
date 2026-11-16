
package com.shivani.shop_api.order;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderHistoryController {

    private final OrderHistoryService orderHistoryService;
    private final OrderItemRepository orderItemRepository;

    public OrderHistoryController(
            OrderHistoryService orderHistoryService,
            OrderItemRepository orderItemRepository) {

        this.orderHistoryService = orderHistoryService;
        this.orderItemRepository = orderItemRepository;
    }

    // View all orders belonging to the logged-in customer
    @GetMapping
    public List<OrderResponse> getOrders(
            Authentication authentication) {

        return orderHistoryService
                .getCustomerOrders(authentication.getName())
                .stream()
                .map(order -> new OrderResponse(
                        order.getId(),
                        order.getStatus().name(),
                        order.getCreatedAt()
                ))
                .toList();
    }

    // View one order with its purchased products
    @GetMapping("/{orderId}")
    public OrderDetailsResponse getOrderDetails(
            Authentication authentication,
            @PathVariable Long orderId) {

        Order order = orderHistoryService.getCustomerOrder(
                authentication.getName(),
                orderId
        );

        List<OrderItemResponse> items = orderItemRepository
                .findByOrderId(order.getId())
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPriceAtPurchase(),
                        item.getSubtotal()
                ))
                .toList();

        double totalAmount = items.stream()
                .mapToDouble(OrderItemResponse::subtotal)
                .sum();

        return new OrderDetailsResponse(
                order.getId(),
                order.getStatus().name(),
                order.getCreatedAt(),
                items,
                totalAmount
        );
    }

    public record OrderResponse(
            Long orderId,
            String status,
            LocalDateTime createdAt
    ) {}

    public record OrderItemResponse(
            Long productId,
            String productName,
            int quantity,
            double priceAtPurchase,
            double subtotal
    ) {}

    public record OrderDetailsResponse(
            Long orderId,
            String status,
            LocalDateTime createdAt,
            List<OrderItemResponse> items,
            double totalAmount
    ) {}
}
