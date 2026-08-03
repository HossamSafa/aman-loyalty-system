package com.aman.acceptance.loyalty.model;

import com.aman.acceptance.loyalty.enums.LotStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "points_lots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private LoyaltyAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "earning_transaction_id", nullable = false)
    private LoyaltyTransaction earningTransaction;

    @Column(name = "original_points", nullable = false)
    private Integer originalPoints;

    @Column(name = "remaining_points", nullable = false)
    private Integer remainingPoints;

    @Column(name = "unlock_at", nullable = false)
    private LocalDateTime unlockAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LotStatus status;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = LotStatus.LOCKED;
        }
    }

    public boolean isSpendable() {
        return status == LotStatus.AVAILABLE
                && remainingPoints > 0
                && expiresAt.isAfter(LocalDateTime.now());
    }

}
