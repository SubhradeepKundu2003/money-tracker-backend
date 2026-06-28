package com.codewithsubhra.money_tracker_backend.user.web;

import com.codewithsubhra.money_tracker_backend.auth.web.dto.UserResponse;
import com.codewithsubhra.money_tracker_backend.common.web.ApiResponse;
import com.codewithsubhra.money_tracker_backend.security.AuthPrincipal;
import com.codewithsubhra.money_tracker_backend.user.BaseCurrencyService;
import com.codewithsubhra.money_tracker_backend.user.web.dto.ChangeBaseCurrencyRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final BaseCurrencyService baseCurrencyService;

    public UserController(BaseCurrencyService baseCurrencyService) {
        this.baseCurrencyService = baseCurrencyService;
    }

    /**
     * Switch the user's base currency, re-denominating all of their accounts,
     * transactions, and budgets into it at the current market rate.
     */
    @PatchMapping("/base-currency")
    public ApiResponse<UserResponse> changeBaseCurrency(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ChangeBaseCurrencyRequest request) {
        return ApiResponse.ok(UserResponse.from(
                baseCurrencyService.changeBaseCurrency(principal.userId(), request.currency())));
    }
}
