package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.dto.request.ProgramRuleRequest;
import com.aman.acceptance.loyalty.model.dto.response.ProgramRuleResponse;
import com.aman.acceptance.loyalty.service.ProgramRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

}
