package com.aman.acceptance.loyalty.repositries;


import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {
    boolean existsById(Long id);


}
