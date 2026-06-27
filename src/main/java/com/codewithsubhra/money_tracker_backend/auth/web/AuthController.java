package com.codewithsubhra.money_tracker_backend.auth.web;

import com.codewithsubhra.money_tracker_backend.auth.AuthService;
import com.codewithsubhra.money_tracker_backend.auth.web.dto.LoginRequest;
import com.codewithsubhra.money_tracker_backend.auth.web.dto.RefreshRequest;
import com.codewithsubhra.money_tracker_backend.auth.web.dto.RegisterRequest;
import com.codewithsubhra.money_tracker_backend.auth.web.dto.SessionResponse;
import com.codewithsubhra.money_tracker_backend.auth.web.dto.UserResponse;
import com.codewithsubhra.money_tracker_backend.common.web.ApiResponse;
import com.codewithsubhra.money_tracker_backend.security.AuthPrincipal;
import com.codewithsubhra.money_tracker_backend.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<SessionResponse>> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        SessionResponse session = authService.register(request, http.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(session));
    }

    @PostMapping("/login")
    public ApiResponse<SessionResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok(authService.login(request, http.getHeader("User-Agent")));
    }

    @PostMapping("/refresh")
    public ApiResponse<SessionResponse> refresh(
            @Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
        return ApiResponse.ok(authService.refresh(request.refreshToken(), http.getHeader("User-Agent")));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestBody(required = false) RefreshRequest request, HttpServletRequest http) {
        String refreshToken = request != null ? request.refreshToken() : null;
        authService.logout(extractBearer(http), refreshToken);
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(UserResponse.from(userService.getById(principal.userId())));
    }

    private String extractBearer(HttpServletRequest http) {
        String header = http.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
