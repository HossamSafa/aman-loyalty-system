package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.Customer;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoyaltyAccountRepository  extends JpaRepository <LoyaltyAccount,Long> {

        Optional<LoyaltyAccount> findByProgramAndCustomer(LoyaltyProgram program , Customer customer);

}
