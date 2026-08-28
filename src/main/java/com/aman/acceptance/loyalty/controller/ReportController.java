package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.dto.MonthlyReportDto;
import com.aman.acceptance.loyalty.model.dto.response.ApiResponse;
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

    @PreAuthorize("hasAuthority('loyalty.admin')")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getSummary(
            @RequestParam Long programId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        ReportSummaryResponse data = reportService.getSummary(programId, from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PreAuthorize("hasAuthority('loyalty.admin')")
    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<List<MonthlyReportDto>>> getMonthlyTrend(
            @RequestParam Long programId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        List<MonthlyReportDto> data = reportService.getMonthlyTrend(programId, from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}