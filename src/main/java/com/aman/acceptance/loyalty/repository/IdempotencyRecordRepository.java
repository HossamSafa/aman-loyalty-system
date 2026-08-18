package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByClientIdAndEndpointAndIdempotencyKey(
            String clientId,
            String endpoint,
            String idempotencyKey
    );
}