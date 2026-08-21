package com.aman.acceptance.loyalty.model;

import com.aman.acceptance.loyalty.enums.CurrencyCode;
import com.aman.acceptance.loyalty.enums.RefundType;
import com.aman.acceptance.loyalty.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import com.aman.acceptance.loyalty.enums.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "loyalty_transactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_account_source_type",
                columnNames = {"account_id", "source_transaction_id", "type"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private LoyaltyAccount account;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;


    @Column(name = "source_transaction_id", nullable = false, length = 100)
    private String sourceTransactionId;


    @Column(name = "points", nullable = false)
    private Integer points;


    @Column(name = "money_amount", precision = 12, scale = 2)
    private BigDecimal moneyAmount;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.COMMITTED;


    @Column(name = "idempotency_key",nullable = false, unique = true, length = 255)
    private String idempotencyKey;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }


@Column(name = "original_source_transaction_id")
private String originalSourceTransactionId;

@Enumerated(EnumType.STRING)
@Column(name = "refund_type")
private RefundType refundType;

@Enumerated(EnumType.STRING)
@Column(name = "currency_code")
private CurrencyCode currency;

@Column(name = "transaction_time")
private LocalDateTime transactionTime;


}
