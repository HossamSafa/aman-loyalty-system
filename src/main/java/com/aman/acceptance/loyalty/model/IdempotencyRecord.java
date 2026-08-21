package com.aman.acceptance.loyalty.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_client_endpoint_key",
                columnNames = {"client_id", "endpoint", "idempotency_key"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "endpoint", nullable = false, length = 255)
    private String endpoint;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;


    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Lob
    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
