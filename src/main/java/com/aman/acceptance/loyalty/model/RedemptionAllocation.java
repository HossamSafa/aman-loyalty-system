package com.aman.acceptance.loyalty.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "redemption_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedemptionAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redemption_id", nullable = false)
    private Redemption redemption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private PointsLot lot;

    @Column(name = "points", nullable = false)
    private Integer points;
}
