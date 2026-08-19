package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.Redemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RedemptionRepository extends JpaRepository<Redemption, Long> {

    Optional<Redemption> findByIdAndAccount_Id(Long id, Long accountId);

}