package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query(value = """
            SELECT id, actor_id, action, entity_type, entity_id, after_json, created_at
            FROM audit_events
            ORDER BY created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> getRecentAuditEvents(@Param("limit") int limit);
}