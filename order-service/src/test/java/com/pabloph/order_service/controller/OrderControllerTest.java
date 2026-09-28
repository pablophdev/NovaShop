package com.pabloph.order_service.controller;

import com.pabloph.order_service.dto.order.CreateOrderRequest;
import com.pabloph.order_service.dto.order.OrderItemResponse;
import com.pabloph.order_service.dto.order.OrderResponse;
import com.pabloph.order_service.entity.OrderStatus;
import com.pabloph.order_service.exception.GlobalExceptionHandler;
import com.pabloph.order_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    void createOrderReturnsCreatedOrder() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(orderResponse(OrderStatus.CONFIRMED));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.items[0].productId").value(10));

        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }

    @Test
    void createOrderReturnsBadRequestWhenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": null,
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors", hasKey("customerId")))
                .andExpect(jsonPath("$.validationErrors", hasKey("items")));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void createOrderReturnsBadRequestWhenItemQuantityIsInvalid() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": 1,
                                  "items": [
                                    { "productId": 10, "quantity": 0 }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void getOrderByIdReturnsOrder() throws Exception {
        when(orderService.getOrderById(100L)).thenReturn(orderResponse(OrderStatus.CONFIRMED));

        mockMvc.perform(get("/api/orders/{id}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getOrderByIdReturnsNotFoundWhenServiceThrowsNotFound() throws Exception {
        when(orderService.getOrderById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        mockMvc.perform(get("/api/orders/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found"))
                .andExpect(jsonPath("$.path").value("/api/orders/99"));
    }

    @Test
    void getOrdersReturnsPage() throws Exception {
        when(orderService.getOrders(any()))
                .thenReturn(new PageImpl<>(List.of(orderResponse(OrderStatus.CONFIRMED)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    @Test
    void cancelOrderReturnsCancelledOrder() throws Exception {
        when(orderService.cancelOrder(100L)).thenReturn(orderResponse(OrderStatus.CANCELLED));

        mockMvc.perform(patch("/api/orders/{id}/cancel", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private static String validOrderJson() {
        return """
                {
                  "customerId": 1,
                  "items": [
                    { "productId": 10, "quantity": 2 }
                  ]
                }
                """;
    }

    private static OrderResponse orderResponse(OrderStatus status) {
        return new OrderResponse(
                100L,
                1L,
                status,
                BigDecimal.valueOf(39.98),
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of(new OrderItemResponse(
                        1L,
                        10L,
                        "Keyboard",
                        BigDecimal.valueOf(19.99),
                        2,
                        BigDecimal.valueOf(39.98)
                ))
        );
    }
}
