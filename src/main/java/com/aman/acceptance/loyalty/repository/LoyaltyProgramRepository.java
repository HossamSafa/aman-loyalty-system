package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.LoyaltyProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoyaltyProgramRepository extends JpaRepository<LoyaltyProgram,Long>{

        Optional<LoyaltyProgram> findByMerchantId(String merchantId);
}
