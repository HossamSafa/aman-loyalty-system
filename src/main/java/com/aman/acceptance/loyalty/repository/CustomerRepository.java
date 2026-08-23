package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface CustomerRepository  extends JpaRepository<Customer,Long> {

    Page<Customer> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Optional<Customer> findByMobileHash(String mobileHash);

}
