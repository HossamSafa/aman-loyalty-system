package com.aman.acceptance.loyalty.model.dto;
import com.aman.acceptance.loyalty.enums.LotStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EarningResponse {
    @Schema(example = "10001")
    private Long loyaltyTransactionId;

    @Schema(example = "sale-20260727-00091")
    private String sourceTransactionId;

    @Schema(example = "1000")
    private Integer earnedPoints;

    private LotStatus pointsStatus;

    private LocalDateTime unlockAt;

    private LocalDateTime expiresAt;

    @Schema(example = "4")
    private Long appliedRuleVersion;

    private BalanceDto balance;
}
