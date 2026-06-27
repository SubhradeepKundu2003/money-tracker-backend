package com.codewithsubhra.money_tracker_backend.transaction.web;

import com.codewithsubhra.money_tracker_backend.common.web.ApiResponse;
import com.codewithsubhra.money_tracker_backend.security.AuthPrincipal;
import com.codewithsubhra.money_tracker_backend.transaction.TransactionService;
import com.codewithsubhra.money_tracker_backend.transaction.domain.TransactionType;
import com.codewithsubhra.money_tracker_backend.transaction.web.dto.TransactionRequest;
import com.codewithsubhra.money_tracker_backend.transaction.web.dto.TransactionResponse;
import com.codewithsubhra.money_tracker_backend.transaction.web.dto.TransactionSummaryResponse;
import com.codewithsubhra.money_tracker_backend.user.UserService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionService transactionService;
    private final UserService userService;

    public TransactionController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<Page<TransactionResponse>> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<TransactionResponse> result = transactionService
                .search(principal.userId(), accountId, type, from, to, pageable)
                .map(TransactionResponse::from);
        return ApiResponse.ok(result);
    }

    @GetMapping("/summary")
    public ApiResponse<TransactionSummaryResponse> summary(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(transactionService.summary(principal.userId(), from, to));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.ok(TransactionResponse.from(transactionService.get(principal.userId(), id)));
    }

    @PostMapping
    public ApiResponse<TransactionResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody TransactionRequest request) {
        var user = userService.getById(principal.userId());
        return ApiResponse.ok(TransactionResponse.from(transactionService.create(user, request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<TransactionResponse> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody TransactionRequest request) {
        return ApiResponse.ok(TransactionResponse.from(transactionService.update(principal.userId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        transactionService.delete(principal.userId(), id);
        return ApiResponse.ok(null);
    }
}
