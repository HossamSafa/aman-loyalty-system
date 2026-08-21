package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.exception.AccountException;
import com.aman.acceptance.loyalty.model.responses.ExpireJobResult;
import com.aman.acceptance.loyalty.service.LoyaltyPointsExpirationJobService;
import com.aman.acceptance.loyalty.service.LoyaltyPointsUnlockJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/loyalty/jobs")
@RequiredArgsConstructor
public class LoyaltyPointsJobController {

    private final LoyaltyPointsUnlockJobService unlockJobService;
    private final LoyaltyPointsExpirationJobService expirationJobService;

    @PostMapping("/expire")
    public ExpireJobResult expirePoints(
            @RequestParam(defaultValue = "1000") int batchSize,
            @RequestParam(defaultValue = "0") Long checkpointId
    ) throws AccountException {
//System.out.println("MMM"+LocalDateTime.now().toString());
        return expirationJobService.processExpireJob(
                LocalDateTime.now(),
                batchSize,
                checkpointId
        );
    }
}