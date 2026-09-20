package com.pabloph.customer_service.service;

import com.pabloph.customer_service.dto.CustomerRequest;
import com.pabloph.customer_service.dto.CustomerResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    CustomerResponse createCustomer(@Valid CustomerRequest request);

    CustomerResponse getCustomerById(Long id);

    Page<CustomerResponse> getCustomers(Pageable pageable);

    CustomerResponse updateCustomer(Long id, @Valid CustomerRequest request);

    void deleteCustomer(Long id);
}
