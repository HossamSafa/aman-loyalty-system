package com.aman.acceptance.loyalty.controller;

import com.aman.acceptance.loyalty.model.request.FreezeAccountRequest;
import com.aman.acceptance.loyalty.model.request.UnfreezeAccountRequest;
import com.aman.acceptance.loyalty.model.response.AccountStatusResponse;
import com.aman.acceptance.loyalty.model.response.ApiResponse;
import com.aman.acceptance.loyalty.service.AccountFreezeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AccountFreezeService accountFreezeService;

    @PostMapping("/{accountId}/freeze")
    public ResponseEntity<ApiResponse<AccountStatusResponse>> freeze(
            @PathVariable Long accountId,
            @Valid @RequestBody FreezeAccountRequest request
            ){
        AccountStatusResponse response = accountFreezeService.freeze(accountId, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }

    @PostMapping("{accountId}/unfreeze")
    public ResponseEntity<ApiResponse<AccountStatusResponse>> unfreeze(
            @PathVariable Long accountId,
            @Valid @RequestBody UnfreezeAccountRequest request
            ){
        AccountStatusResponse response = accountFreezeService.unfreeze(accountId, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }

}
