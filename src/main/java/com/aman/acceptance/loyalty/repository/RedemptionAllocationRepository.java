package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.RedemptionAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedemptionAllocationRepository extends JpaRepository<RedemptionAllocation, Long> {
}
