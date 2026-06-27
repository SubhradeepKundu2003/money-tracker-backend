package com.codewithsubhra.money_tracker_backend.account.web;

import com.codewithsubhra.money_tracker_backend.account.AccountService;
import com.codewithsubhra.money_tracker_backend.account.web.dto.AccountRequest;
import com.codewithsubhra.money_tracker_backend.account.web.dto.AccountResponse;
import com.codewithsubhra.money_tracker_backend.common.web.ApiResponse;
import com.codewithsubhra.money_tracker_backend.security.AuthPrincipal;
import com.codewithsubhra.money_tracker_backend.user.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final UserService userService;

    public AccountController(AccountService accountService, UserService userService) {
        this.accountService = accountService;
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<AccountResponse>> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(accountService.list(principal.userId()).stream()
                .map(AccountResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<AccountResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.ok(AccountResponse.from(accountService.get(principal.userId(), id)));
    }

    @PostMapping
    public ApiResponse<AccountResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody AccountRequest request) {
        var user = userService.getById(principal.userId());
        return ApiResponse.ok(AccountResponse.from(accountService.create(user, request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<AccountResponse> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AccountRequest request) {
        return ApiResponse.ok(AccountResponse.from(accountService.update(principal.userId(), id, request)));
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<AccountResponse> archive(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean archived) {
        return ApiResponse.ok(AccountResponse.from(accountService.setArchived(principal.userId(), id, archived)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        accountService.delete(principal.userId(), id);
        return ApiResponse.ok(null);
    }
}
