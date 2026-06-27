package com.codewithsubhra.money_tracker_backend.budget.web;

import com.codewithsubhra.money_tracker_backend.budget.BudgetService;
import com.codewithsubhra.money_tracker_backend.budget.web.dto.BudgetRequest;
import com.codewithsubhra.money_tracker_backend.budget.web.dto.BudgetResponse;
import com.codewithsubhra.money_tracker_backend.budget.web.dto.SetActiveRequest;
import com.codewithsubhra.money_tracker_backend.budget.web.dto.TopUpRequest;
import com.codewithsubhra.money_tracker_backend.common.web.ApiResponse;
import com.codewithsubhra.money_tracker_backend.security.AuthPrincipal;
import com.codewithsubhra.money_tracker_backend.user.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final UserService userService;

    public BudgetController(BudgetService budgetService, UserService userService) {
        this.budgetService = budgetService;
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<BudgetResponse>> list(@AuthenticationPrincipal AuthPrincipal principal) {
        List<BudgetResponse> result = budgetService.list(principal.userId()).stream()
                .map(BudgetResponse::from)
                .toList();
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<BudgetResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.ok(BudgetResponse.from(budgetService.get(principal.userId(), id)));
    }

    @PostMapping
    public ApiResponse<BudgetResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody BudgetRequest request) {
        var user = userService.getById(principal.userId());
        return ApiResponse.ok(BudgetResponse.from(budgetService.create(user, request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<BudgetResponse> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody BudgetRequest request) {
        return ApiResponse.ok(BudgetResponse.from(budgetService.update(principal.userId(), id, request)));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<BudgetResponse> setActive(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody SetActiveRequest request) {
        return ApiResponse.ok(
                BudgetResponse.from(budgetService.setActive(principal.userId(), id, request.active())));
    }

    @PostMapping("/{id}/top-up")
    public ApiResponse<BudgetResponse> topUp(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody TopUpRequest request) {
        return ApiResponse.ok(
                BudgetResponse.from(budgetService.topUp(principal.userId(), id, request.amount())));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        budgetService.delete(principal.userId(), id);
        return ApiResponse.ok(null);
    }
}
