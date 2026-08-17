package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.model.dto.request.CustomerRequestDto;
import com.aman.acceptance.loyalty.model.dto.response.CustomerDto;
import com.aman.acceptance.loyalty.exception.LoyaltyException;
import com.aman.acceptance.loyalty.mapper.CustomerMapper;
import com.aman.acceptance.loyalty.model.Customer;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyProgram;
import com.aman.acceptance.loyalty.repository.CustomerRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyProgramRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Mock
    private LoyaltyProgramRepository loyaltyProgramRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(customerService, "secretKey", "1234567890123456");
    }

    @Test
    void resolve_newCustomer_createsCustomerAndAccount() {


        CustomerRequestDto request = new CustomerRequestDto();
        request.setMobileNumber("01012345678");
        request.setCustomerName("Ahmed Ali");
        request.setAutoEnroll(true);

        Long programId = 1L;

        LoyaltyProgram program = LoyaltyProgram.builder()
                .id(programId)
                .build();

        Customer savedCustomer = Customer.builder()
                .id(1L)
                .build();

        LoyaltyAccount savedAccount = LoyaltyAccount.builder()
                .id(1L)
                .program(program)
                .customer(savedCustomer)
                .build();

        CustomerDto expectedResponse = CustomerDto.builder()
                .customerId(1L)
                .accountId(1L)
                .newlyEnrolled(true)
                .build();

        when(customerRepository.findByMobileHash(anyString())).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(loyaltyProgramRepository.findById(programId)).thenReturn(Optional.of(program));
        when(loyaltyAccountRepository.findByProgramAndCustomer(program, savedCustomer)).thenReturn(Optional.empty());
        when(loyaltyAccountRepository.save(any(LoyaltyAccount.class))).thenReturn(savedAccount);
        when(customerMapper.toResolveCustomerResponse(eq(savedCustomer), eq(savedAccount), eq(true), anyString())).thenReturn(expectedResponse);


        CustomerDto result = customerService.resolve(request, programId);
        assertNotNull(result);
        assertTrue(result.getNewlyEnrolled());
        assertEquals(1L, result.getCustomerId());

        verify(customerRepository, times(1)).save(any(Customer.class));
        verify(loyaltyAccountRepository, times(1)).save(any(LoyaltyAccount.class));



    }

    @Test
    void resolve_existingCustomerAndAccount_returnsExistingData() {

        // Arrange
        CustomerRequestDto request = new CustomerRequestDto();
        request.setMobileNumber("01012345678");
        request.setCustomerName("Ahmed Ali");
        request.setAutoEnroll(true);

        Long programId = 1L;

        LoyaltyProgram program = LoyaltyProgram.builder()
                .id(programId)
                .build();

        Customer existingCustomer = Customer.builder()
                .id(1L)
                .build();

        LoyaltyAccount existingAccount = LoyaltyAccount.builder()
                .id(1L)
                .program(program)
                .customer(existingCustomer)
                .build();

        CustomerDto expectedResponse = CustomerDto.builder()
                .customerId(1L)
                .accountId(1L)
                .newlyEnrolled(false)
                .build();

        when(customerRepository.findByMobileHash(anyString())).thenReturn(Optional.of(existingCustomer));
        when(loyaltyProgramRepository.findById(programId)).thenReturn(Optional.of(program));
        when(loyaltyAccountRepository.findByProgramAndCustomer(program, existingCustomer)).thenReturn(Optional.of(existingAccount));
        when(customerMapper.toResolveCustomerResponse(eq(existingCustomer), eq(existingAccount), eq(false), anyString())).thenReturn(expectedResponse);

        // Act
        CustomerDto result = customerService.resolve(request, programId);

        // Assert
        assertNotNull(result);
        assertFalse(result.getNewlyEnrolled());
        assertEquals(1L, result.getCustomerId());

        verify(customerRepository, never()).save(any(Customer.class));
        verify(loyaltyAccountRepository, never()).save(any(LoyaltyAccount.class));
    }

    @Test
    void resolve_customerNotFoundAndAutoEnrollFalse_throwsException() {

        // Arrange
        CustomerRequestDto request = new CustomerRequestDto();
        request.setMobileNumber("01012345678");
        request.setAutoEnroll(false);

        Long programId = 1L;

        when(customerRepository.findByMobileHash(anyString())).thenReturn(Optional.empty());

        // Act + Assert
        LoyaltyException exception = assertThrows(LoyaltyException.class, () -> {
            customerService.resolve(request, programId);
        });

        assertEquals("LOYALTY_ACCOUNT_NOT_FOUND", exception.getErrorCode());

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void resolve_existingCustomerNewAccount_createsAccountOnly() {

        // Arrange
        CustomerRequestDto request = new CustomerRequestDto();
        request.setMobileNumber("01012345678");
        request.setAutoEnroll(true);

        Long programId = 2L;

        LoyaltyProgram program = LoyaltyProgram.builder()
                .id(programId)
                .build();

        Customer existingCustomer = Customer.builder()
                .id(1L)
                .build();

        LoyaltyAccount newAccount = LoyaltyAccount.builder()
                .id(5L)
                .program(program)
                .customer(existingCustomer)
                .build();

        CustomerDto expectedResponse = CustomerDto.builder()
                .customerId(1L)
                .accountId(5L)
                .newlyEnrolled(true)
                .build();

        when(customerRepository.findByMobileHash(anyString())).thenReturn(Optional.of(existingCustomer));
        when(loyaltyProgramRepository.findById(programId)).thenReturn(Optional.of(program));
        when(loyaltyAccountRepository.findByProgramAndCustomer(program, existingCustomer)).thenReturn(Optional.empty());
        when(loyaltyAccountRepository.save(any(LoyaltyAccount.class))).thenReturn(newAccount);
        when(customerMapper.toResolveCustomerResponse(eq(existingCustomer), eq(newAccount), eq(true), anyString())).thenReturn(expectedResponse);

        // Act
        CustomerDto result = customerService.resolve(request, programId);

        // Assert
        assertTrue(result.getNewlyEnrolled());

        verify(customerRepository, never()).save(any(Customer.class));
        verify(loyaltyAccountRepository, times(1)).save(any(LoyaltyAccount.class));
    }
}