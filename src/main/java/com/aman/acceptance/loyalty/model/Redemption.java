package com.aman.acceptance.loyalty.model;

import com.aman.acceptance.loyalty.enums.RedemptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "redemptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Redemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private LoyaltyAccount account;

    @Column(name = "purchase_transaction_id", nullable = false, unique = true, length = 100)
    private String purchaseTransactionId;

    @Column(name = "requested_points", nullable = false)
    private Integer requestedPoints;

    @Column(name = "discount_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RedemptionStatus status;

    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @Column(name = "otp_attempts_remaining")
    private Integer otpAttemptsRemaining;

    @Column(name = "authorization_code", length = 30)
    private String authorizationCode;

    @Column(name = "reservation_expires_at")
    private LocalDateTime reservationExpiresAt;

    @OneToMany(mappedBy = "redemption", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RedemptionAllocation> allocations = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = RedemptionStatus.OTP_PENDING;
        }
    }

    public void addAllocation(RedemptionAllocation allocation) {
        allocations.add(allocation);
        allocation.setRedemption(this);
    }
}
