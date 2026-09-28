package com.pabloph.order_service.service;

import com.pabloph.order_service.client.CustomerClient;
import com.pabloph.order_service.client.ProductClient;
import com.pabloph.order_service.dto.client.CustomerResponse;
import com.pabloph.order_service.dto.client.ProductResponse;
import com.pabloph.order_service.dto.client.UpdateStockRequest;
import com.pabloph.order_service.dto.order.CreateOrderItemRequest;
import com.pabloph.order_service.dto.order.CreateOrderRequest;
import com.pabloph.order_service.dto.order.OrderResponse;
import com.pabloph.order_service.entity.Order;
import com.pabloph.order_service.entity.OrderItem;
import com.pabloph.order_service.entity.OrderStatus;
import com.pabloph.order_service.repository.OrderRepository;
import com.pabloph.order_service.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private ProductClient productClient;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, customerClient, productClient);
    }

    @Test
    void createOrderValidatesCustomerAndProductUpdatesStockAndConfirmsOrder() {
        CreateOrderRequest request = createOrderRequest();
        when(customerClient.getCustomerById(1L)).thenReturn(customer(true));
        when(productClient.getProductById(10L)).thenReturn(product(10));
        when(productClient.updateStock(any(), any(UpdateStockRequest.class))).thenReturn(product(8));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            int itemId = 1;
            for (OrderItem item : order.getItems()) {
                item.setId((long) itemId++);
            }
            return order;
        });

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.customerId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.total()).isEqualByComparingTo("39.98");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productId()).isEqualTo(10L);
        assertThat(response.items().get(0).quantity()).isEqualTo(2);

        ArgumentCaptor<UpdateStockRequest> stockCaptor = ArgumentCaptor.forClass(UpdateStockRequest.class);
        verify(productClient).updateStock(org.mockito.ArgumentMatchers.eq(10L), stockCaptor.capture());
        assertThat(stockCaptor.getValue().stock()).isEqualTo(8);
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void createOrderThrowsBadRequestWhenCustomerIsInactive() {
        CreateOrderRequest request = createOrderRequest();
        when(customerClient.getCustomerById(1L)).thenReturn(customer(false));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(productClient, never()).getProductById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderThrowsBadRequestWhenProductHasInsufficientStock() {
        CreateOrderRequest request = createOrderRequest();
        when(customerClient.getCustomerById(1L)).thenReturn(customer(true));
        when(productClient.getProductById(10L)).thenReturn(product(1));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(productClient, never()).updateStock(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderByIdReturnsOrderWhenFound() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order(OrderStatus.CONFIRMED)));

        OrderResponse response = orderService.getOrderById(100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void getOrderByIdThrowsNotFoundWhenMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getOrdersReturnsMappedPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order(OrderStatus.CONFIRMED)), pageable, 1));

        Page<OrderResponse> response = orderService.getOrders(pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelOrderCancelsConfirmedOrderAndRestoresStock() {
        Order order = order(OrderStatus.CONFIRMED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(productClient.getProductById(10L)).thenReturn(product(8));
        when(productClient.updateStock(any(), any(UpdateStockRequest.class))).thenReturn(product(10));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder(100L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);

        ArgumentCaptor<UpdateStockRequest> stockCaptor = ArgumentCaptor.forClass(UpdateStockRequest.class);
        verify(productClient).updateStock(org.mockito.ArgumentMatchers.eq(10L), stockCaptor.capture());
        assertThat(stockCaptor.getValue().stock()).isEqualTo(10);
    }

    @Test
    void cancelOrderThrowsBadRequestWhenOrderIsAlreadyCancelled() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order(OrderStatus.CANCELLED)));

        assertThatThrownBy(() -> orderService.cancelOrder(100L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(productClient, never()).updateStock(any(), any());
        verify(orderRepository, never()).save(any());
    }

    private static CreateOrderRequest createOrderRequest() {
        return new CreateOrderRequest(1L, List.of(new CreateOrderItemRequest(10L, 2)));
    }

    private static CustomerResponse customer(Boolean active) {
        return new CustomerResponse(
                1L,
                "Pedro",
                "Perez",
                "pedro@example.com",
                "+56912345678",
                "Av. Providencia 123",
                active,
                LocalDateTime.now()
        );
    }

    private static ProductResponse product(Integer stock) {
        return new ProductResponse(
                10L,
                "Keyboard",
                "KEY-001",
                "Mechanical keyboard",
                BigDecimal.valueOf(19.99),
                stock,
                true,
                "Electronics",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private static Order order(OrderStatus status) {
        Order order = new Order();
        order.setId(100L);
        order.setCustomerId(1L);
        order.setStatus(status);
        order.setTotal(BigDecimal.valueOf(39.98));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProductId(10L);
        item.setProductName("Keyboard");
        item.setUnitPrice(BigDecimal.valueOf(19.99));
        item.setQuantity(2);
        item.setSubtotal(BigDecimal.valueOf(39.98));
        order.setItems(List.of(item));
        return order;
    }
}
