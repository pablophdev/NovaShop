package com.pabloph.customer_service.controller;

import com.pabloph.customer_service.dto.CustomerRequest;
import com.pabloph.customer_service.dto.CustomerResponse;
import com.pabloph.customer_service.exception.GlobalExceptionHandler;
import com.pabloph.customer_service.service.CustomerService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new CustomerController(customerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    void createCustomerReturnsCreatedCustomer() throws Exception {
        when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(customerResponse());

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCustomerJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("pedro@example.com"))
                .andExpect(jsonPath("$.active").value(true));

        verify(customerService).createCustomer(any(CustomerRequest.class));
    }

    @Test
    void createCustomerReturnsBadRequestWhenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "",
                                  "lastName": "",
                                  "email": "not-an-email",
                                  "phone": "",
                                  "address": "Av. Providencia 123",
                                  "active": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors", hasKey("firstName")))
                .andExpect(jsonPath("$.validationErrors", hasKey("lastName")))
                .andExpect(jsonPath("$.validationErrors", hasKey("email")))
                .andExpect(jsonPath("$.validationErrors", hasKey("phone")))
                .andExpect(jsonPath("$.validationErrors", hasKey("active")));

        verify(customerService, never()).createCustomer(any());
    }

    @Test
    void getCustomerByIdReturnsCustomer() throws Exception {
        when(customerService.getCustomerById(1L)).thenReturn(customerResponse());

        mockMvc.perform(get("/api/customers/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("pedro@example.com"));
    }

    @Test
    void getCustomerByIdReturnsNotFoundWhenServiceThrowsNotFound() throws Exception {
        when(customerService.getCustomerById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        mockMvc.perform(get("/api/customers/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found"))
                .andExpect(jsonPath("$.path").value("/api/customers/99"));
    }

    @Test
    void getCustomersReturnsPage() throws Exception {
        when(customerService.getCustomers(any()))
                .thenReturn(new PageImpl<>(List.of(customerResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].email").value("pedro@example.com"));
    }

    @Test
    void updateCustomerReturnsUpdatedCustomer() throws Exception {
        CustomerResponse response = new CustomerResponse(
                1L,
                "Pedro",
                "Perez",
                "pedro@example.com",
                "+56912345678",
                "Av. Providencia 123",
                false,
                LocalDateTime.now()
        );
        when(customerService.updateCustomer(eq(1L), any(CustomerRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/customers/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Pedro",
                                  "lastName": "Perez",
                                  "email": "pedro@example.com",
                                  "phone": "+56912345678",
                                  "address": "Av. Providencia 123",
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deleteCustomerReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/customers/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(customerService).deleteCustomer(1L);
    }

    private static String validCustomerJson() {
        return """
                {
                  "firstName": "Pedro",
                  "lastName": "Perez",
                  "email": "pedro@example.com",
                  "phone": "+56912345678",
                  "address": "Av. Providencia 123",
                  "active": true
                }
                """;
    }

    private static CustomerResponse customerResponse() {
        return new CustomerResponse(
                1L,
                "Pedro",
                "Perez",
                "pedro@example.com",
                "+56912345678",
                "Av. Providencia 123",
                true,
                LocalDateTime.now()
        );
    }
}
