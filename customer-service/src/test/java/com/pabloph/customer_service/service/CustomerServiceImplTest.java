package com.pabloph.customer_service.service;

import com.pabloph.customer_service.dto.CustomerRequest;
import com.pabloph.customer_service.dto.CustomerResponse;
import com.pabloph.customer_service.entity.Customer;
import com.pabloph.customer_service.repository.CustomerRepository;
import com.pabloph.customer_service.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository);
    }

    @Test
    void createCustomerSavesCustomerWhenEmailIsAvailable() {
        CustomerRequest request = customerRequest("pedro@example.com", true);
        when(customerRepository.existsByEmail("pedro@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(1L);
            return customer;
        });

        CustomerResponse response = customerService.createCustomer(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("pedro@example.com");
        assertThat(response.firstName()).isEqualTo("Pedro");
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void createCustomerThrowsBadRequestWhenEmailAlreadyExists() {
        CustomerRequest request = customerRequest("pedro@example.com", true);
        when(customerRepository.existsByEmail("pedro@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void getCustomerByIdReturnsCustomerWhenFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer(1L, "pedro@example.com", true)));

        CustomerResponse response = customerService.getCustomerById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("pedro@example.com");
    }

    @Test
    void getCustomerByIdThrowsNotFoundWhenMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateCustomerKeepsSameEmailWithoutDuplicateValidation() {
        Customer customer = customer(1L, "pedro@example.com", true);
        CustomerRequest request = customerRequest("pedro@example.com", false);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.updateCustomer(1L, request);

        assertThat(response.active()).isFalse();
        assertThat(response.email()).isEqualTo("pedro@example.com");
        verify(customerRepository, never()).existsByEmail(any());
    }

    @Test
    void updateCustomerValidatesNewEmail() {
        Customer customer = customer(1L, "old@example.com", true);
        CustomerRequest request = customerRequest("new@example.com", true);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.updateCustomer(1L, request);

        assertThat(response.email()).isEqualTo("new@example.com");
        verify(customerRepository).existsByEmail("new@example.com");
    }

    @Test
    void getCustomersReturnsMappedPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(customerRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(customer(1L, "pedro@example.com", true)), pageable, 1));

        Page<CustomerResponse> response = customerService.getCustomers(pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().getFirst().email()).isEqualTo("pedro@example.com");
    }

    @Test
    void deleteCustomerDeletesExistingCustomer() {
        Customer customer = customer(1L, "pedro@example.com", true);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(1L);

        verify(customerRepository).delete(customer);
    }

    private static CustomerRequest customerRequest(String email, Boolean active) {
        return new CustomerRequest(
                "Pedro",
                "Perez",
                email,
                "+56912345678",
                "Av. Providencia 123",
                active
        );
    }

    private static Customer customer(Long id, String email, Boolean active) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFirstName("Pedro");
        customer.setLastName("Perez");
        customer.setEmail(email);
        customer.setPhone("+56912345678");
        customer.setAddress("Av. Providencia 123");
        customer.setActive(active);
        customer.setCreatedAt(LocalDateTime.now());
        return customer;
    }
}
