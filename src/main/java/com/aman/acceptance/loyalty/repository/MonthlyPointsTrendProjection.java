package com.aman.acceptance.loyalty.repository;

import java.time.LocalDateTime;

public interface MonthlyPointsTrendProjection {

    LocalDateTime getMonth();

    Long getPointsIssued();

    Long getPointsRedeemed();

    Long getPointsExpired();
}