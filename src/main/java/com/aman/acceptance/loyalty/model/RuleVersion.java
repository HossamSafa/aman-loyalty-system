package com.aman.acceptance.loyalty.model;

import com.aman.acceptance.loyalty.enums.RoundingMode;
import com.aman.acceptance.loyalty.enums.RuleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "rule_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private LoyaltyProgram program;

    @Column(name = "earning_rate", precision = 6, scale = 2, nullable = false)
    private BigDecimal earningRate;

    @Column(name = "redemption_rate", precision = 6, scale = 4, nullable = false)
    private BigDecimal redemptionRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "rounding_mode", nullable = false)
    @Builder.Default
    private RoundingMode roundingMode = RoundingMode.FLOOR;


    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RuleStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "lock_days", nullable = false)
    private Integer lockDays;

    @Column(name = "expiry_days", nullable = false)
    private Integer expiryDays;

    @Column(name = "minimum_redemption_points", nullable = false)
    private Integer minimumRedemptionPoints;

    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isEffectiveAt(LocalDateTime time) {
        boolean afterStart = !time.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || time.isBefore(effectiveTo);
        return afterStart && beforeEnd;
    }

}
