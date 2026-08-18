package com.aman.acceptance.loyalty.model;

import com.aman.acceptance.loyalty.enums.ProgramStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_programs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, length = 100)
    private String merchantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProgramStatus status;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "lock_days", nullable = false)
    private Integer lockDays;

    @Column(name = "expiry_days", nullable = false)
    private Integer expiryDays;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;



    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ProgramStatus.ACTIVE;
        }
    }

}
