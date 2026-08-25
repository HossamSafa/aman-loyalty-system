package com.aman.acceptance.loyalty.model;

import com.aman.acceptance.loyalty.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "loyalty_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_program_customer",
                columnNames = {"program_id", "customer_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private LoyaltyProgram program;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "available_points", nullable = false)
    @Builder.Default
    private Integer availablePoints = 0;

    @Column(name = "locked_points", nullable = false)
    @Builder.Default
    private Integer lockedPoints = 0;

    @Column(name = "reserved_points", nullable = false)
    @Builder.Default
    private Integer reservedPoints = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LoyaltyAccount(LoyaltyProgram program, Customer customer) {
        this.program = program;
        this.customer = customer;
        this.availablePoints = 0;
        this.lockedPoints = 0;
        this.reservedPoints = 0;
    }

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if(this.status == null){
            this.status = AccountStatus.ACTIVE;
        }
    }


    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }


    public Integer getTotalOwned() {
        return availablePoints + lockedPoints + reservedPoints;
    }

    public void reserveForRedemption(int points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Points to reserve must be positive");
        }
        if (this.availablePoints < points) {
            throw new IllegalStateException("Insufficient available points to reserve " + points);
        }
        this.availablePoints -= points;
        this.reservedPoints += points;
    }

    public void finalizeReservation(int points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Points to finalize must be positive");
        }
        if (this.reservedPoints < points) {
            throw new IllegalStateException("Insufficient reserved points to finalize " + points);
        }
        this.reservedPoints -= points;
    }

    public void releaseReservation(int points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Points to release must be positive");
        }
        if (this.reservedPoints < points) {
            throw new IllegalStateException("Insufficient reserved points to release " + points);
        }
        this.reservedPoints -= points;
        this.availablePoints += points;
    }
}
