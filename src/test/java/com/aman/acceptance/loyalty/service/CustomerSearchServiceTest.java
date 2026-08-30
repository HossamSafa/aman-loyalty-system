package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.model.Customer;
import com.aman.acceptance.loyalty.model.response.PagedResponse;
import com.aman.acceptance.loyalty.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerSearchServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerSearchService customerSearchService;

    private final Pageable pageable = PageRequest.of(0, 10);

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        customerSearchService = new CustomerSearchService(customerRepository);
    }

    @Test
    void search_whenInputLooksLikeMobile_shouldSearchByHash() {
        Customer customer = Customer.builder().id(1L).name("Ahmed Mohamed").build();
        when(customerRepository.findByMobileHash(anyString())).thenReturn(Optional.of(customer));

        PagedResponse<?> result = customerSearchService.search("+201012345678", pageable);

        assertThat(result.getItems()).hasSize(1);
        verify(customerRepository).findByMobileHash(anyString());
        verify(customerRepository, never()).findByNameContainingIgnoreCase(anyString(), any());
    }

    @Test
    void search_whenInputIsName_shouldSearchByName() {
        Customer customer = Customer.builder().id(2L).name("Sara Ali").build();
        when(customerRepository.findByNameContainingIgnoreCase(eq("Sara"), any()))
                .thenReturn(new PageImpl<>(List.of(customer)));

        PagedResponse<?> result = customerSearchService.search("Sara", pageable);

        assertThat(result.getItems()).hasSize(1);
        verify(customerRepository).findByNameContainingIgnoreCase(eq("Sara"), any());
        verify(customerRepository, never()).findByMobileHash(anyString());
    }

    @Test
    void search_whenNoSearchTerm_shouldReturnAllCustomers() {
        when(customerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        customerSearchService.search(null, pageable);

        verify(customerRepository).findAll(pageable);
        verify(customerRepository, never()).findByMobileHash(anyString());
        verify(customerRepository, never()).findByNameContainingIgnoreCase(anyString(), any());
    }
}