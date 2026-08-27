package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.dto.response.MonthlyReportResponse;
import com.aman.acceptance.loyalty.model.dto.response.ReportSummaryResponse;
import com.aman.acceptance.loyalty.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // Flow 11 - Merchant Reporting
    @PreAuthorize("hasAuthority('loyalty.admin')")
    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryResponse> getSummary(
            @RequestParam Long programId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {

        ReportSummaryResponse response =
                reportService.getSummary(
                        programId,
                        from,
                        to
                );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('loyalty.admin')")
    @GetMapping("/trend")
    public ResponseEntity<List<MonthlyReportResponse>> getMonthlyTrend(
            @RequestParam Long programId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {

        List<MonthlyReportResponse> response =
                reportService.getMonthlyTrend(
                        programId,
                        from,
                        to
                );

        return ResponseEntity.ok(response);
    }
}