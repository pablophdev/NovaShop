package com.pabloph.order_service.service.impl;

import com.pabloph.order_service.client.CustomerClient;
import com.pabloph.order_service.client.ProductClient;
import com.pabloph.order_service.dto.client.CustomerResponse;
import com.pabloph.order_service.dto.client.ProductResponse;
import com.pabloph.order_service.dto.client.UpdateStockRequest;
import com.pabloph.order_service.dto.order.CreateOrderItemRequest;
import com.pabloph.order_service.dto.order.CreateOrderRequest;
import com.pabloph.order_service.dto.order.OrderItemResponse;
import com.pabloph.order_service.dto.order.OrderResponse;
import com.pabloph.order_service.entity.Order;
import com.pabloph.order_service.entity.OrderItem;
import com.pabloph.order_service.entity.OrderStatus;
import com.pabloph.order_service.repository.OrderRepository;
import com.pabloph.order_service.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Validated
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;

    public OrderServiceImpl(OrderRepository orderRepository, CustomerClient customerClient, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.customerClient = customerClient;
        this.productClient = productClient;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        CustomerResponse customer = customerClient.getCustomerById(request.customerId());
        if (!Boolean.TRUE.equals(customer.active())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer is not active");
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (CreateOrderItemRequest itemRequest : request.items()) {
            ProductResponse product = productClient.getProductById(itemRequest.productId());
            validateProduct(product, itemRequest.quantity());

            BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(itemRequest.quantity()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(product.id());
            item.setProductName(product.name());
            item.setUnitPrice(product.price());
            item.setQuantity(itemRequest.quantity());
            item.setSubtotal(subtotal);

            items.add(item);
            total = total.add(subtotal);
        }

        order.setTotal(total);
        order.setItems(items);

        Order savedOrder = orderRepository.save(order);
        updateProductsStock(request.items());
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        savedOrder.setUpdatedAt(LocalDateTime.now());

        return toResponse(orderRepository.save(savedOrder));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        return toResponse(getOrder(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public OrderResponse cancelOrder(Long id) {
        Order order = getOrder(id);

        if (OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is already cancelled");
        }

        boolean shouldRestoreStock = OrderStatus.CONFIRMED.equals(order.getStatus());

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        if (shouldRestoreStock) {
            restoreProductsStock(order.getItems());
        }

        return toResponse(orderRepository.save(order));
    }

    private Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private void validateProduct(ProductResponse product, Integer quantity) {
        if (!Boolean.TRUE.equals(product.active())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product is not active");
        }

        if (product.stock() < quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient product stock");
        }
    }

    private void updateProductsStock(List<CreateOrderItemRequest> items) {
        for (CreateOrderItemRequest item : items) {
            ProductResponse product = productClient.getProductById(item.productId());
            productClient.updateStock(product.id(), new UpdateStockRequest(product.stock() - item.quantity()));
        }
    }

    private void restoreProductsStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            ProductResponse product = productClient.getProductById(item.getProductId());
            productClient.updateStock(product.id(), new UpdateStockRequest(product.stock() + item.getQuantity()));
        }
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream().map(this::toItemResponse).toList()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}
