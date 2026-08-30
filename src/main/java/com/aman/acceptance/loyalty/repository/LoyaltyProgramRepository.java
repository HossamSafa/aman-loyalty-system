package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.LoyaltyProgram;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface LoyaltyProgramRepository extends JpaRepository<LoyaltyProgram,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM LoyaltyProgram p
        WHERE p.id = :programId
        """)
    Optional<LoyaltyProgram> findByIdForUpdate(
            @Param("programId") Long programId
    );
        Optional<LoyaltyProgram> findByMerchantId(String merchantId);
}
