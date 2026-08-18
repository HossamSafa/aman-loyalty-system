package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
}
