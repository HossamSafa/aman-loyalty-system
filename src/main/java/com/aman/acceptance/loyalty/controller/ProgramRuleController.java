package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.dto.request.ProgramRuleRequest;
import com.aman.acceptance.loyalty.model.dto.response.ProgramRuleHistoryResponse;
import com.aman.acceptance.loyalty.model.dto.response.ProgramRuleResponse;
import com.aman.acceptance.loyalty.service.ProgramRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.aman.acceptance.loyalty.model.dto.response.ProgramResponse;
import com.aman.acceptance.loyalty.model.dto.response.ProgramRuleDetailsResponse;

import java.util.List;
@RestController
@RequestMapping("/admin/programs")
@RequiredArgsConstructor
public class ProgramRuleController {

    private final ProgramRuleService programRuleService;

    @PreAuthorize("hasAuthority('loyalty.admin')")
    @PutMapping("/{programId}/rules")
    public ResponseEntity<ProgramRuleResponse> updateRules(
            @PathVariable Long programId,
            @RequestBody ProgramRuleRequest request
    ) {

        ProgramRuleResponse response =
                programRuleService.updateRules(programId, request);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('loyalty.admin')")
    @GetMapping
    public ResponseEntity<List<ProgramResponse>> getPrograms() {

        List<ProgramResponse> programs =
                programRuleService.getPrograms();

        return ResponseEntity.ok(programs);
    }
    @PreAuthorize("hasAuthority('loyalty.admin')")
    @GetMapping("/{programId}/rules")
    public ResponseEntity<List<ProgramRuleDetailsResponse>> getProgramRules(
            @PathVariable Long programId
    ) {

        List<ProgramRuleDetailsResponse> rules =
                programRuleService.getProgramRules(programId);

        return ResponseEntity.ok(rules);
    }
    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('loyalty.admin')")
    public ResponseEntity<List<ProgramRuleHistoryResponse>>
    getAllProgramRules() {

        return ResponseEntity.ok(
                programRuleService.getAllProgramRules()
        );
    }
}
