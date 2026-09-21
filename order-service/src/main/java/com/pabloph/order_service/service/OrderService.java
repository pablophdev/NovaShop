package com.pabloph.order_service.service;

import com.pabloph.order_service.dto.order.CreateOrderRequest;
import com.pabloph.order_service.dto.order.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse createOrder(@Valid CreateOrderRequest request);

    OrderResponse getOrderById(Long id);

    Page<OrderResponse> getOrders(Pageable pageable);

    OrderResponse cancelOrder(Long id);
}
