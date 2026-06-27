package com.codewithsubhra.money_tracker_backend.auth;

import com.codewithsubhra.money_tracker_backend.auth.web.dto.LoginRequest;
import com.codewithsubhra.money_tracker_backend.auth.web.dto.RegisterRequest;
import com.codewithsubhra.money_tracker_backend.auth.web.dto.SessionResponse;
import com.codewithsubhra.money_tracker_backend.auth.web.dto.UserResponse;
import com.codewithsubhra.money_tracker_backend.category.CategoryService;
import com.codewithsubhra.money_tracker_backend.common.exception.BadRequestException;
import com.codewithsubhra.money_tracker_backend.common.exception.UnauthorizedException;
import com.codewithsubhra.money_tracker_backend.security.refresh.IssuedRefreshToken;
import com.codewithsubhra.money_tracker_backend.security.refresh.RefreshTokenService;
import com.codewithsubhra.money_tracker_backend.security.session.IssuedSession;
import com.codewithsubhra.money_tracker_backend.security.session.SessionService;
import com.codewithsubhra.money_tracker_backend.user.UserRepository;
import com.codewithsubhra.money_tracker_backend.user.domain.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final RefreshTokenService refreshTokenService;
    private final CategoryService categoryService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
            SessionService sessionService, RefreshTokenService refreshTokenService,
            CategoryService categoryService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.refreshTokenService = refreshTokenService;
        this.categoryService = categoryService;
    }

    @Transactional
    public SessionResponse register(RegisterRequest request, String userAgent) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("An account with this email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        users.save(user);

        categoryService.seedDefaultsFor(user);

        return issueSession(user, userAgent);
    }

    @Transactional
    public SessionResponse login(LoginRequest request, String userAgent) {
        User user = users.findByEmailIgnoreCase(request.email().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return issueSession(user, userAgent);
    }

    /** Exchanges a valid refresh token for a new session + rotated refresh token. */
    @Transactional
    public SessionResponse refresh(String refreshToken, String userAgent) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        IssuedSession session = sessionService.createSession(rotation.user(), userAgent);
        return new SessionResponse(
                session.token(), session.expiresAt(),
                rotation.issued().token(), rotation.issued().expiresAt(),
                null);
    }

    @Transactional
    public void logout(String sessionToken, String refreshToken) {
        if (sessionToken != null) {
            sessionService.revoke(sessionToken);
        }
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }
    }

    private SessionResponse issueSession(User user, String userAgent) {
        IssuedSession session = sessionService.createSession(user, userAgent);
        IssuedRefreshToken refresh = refreshTokenService.issue(user);
        return new SessionResponse(
                session.token(), session.expiresAt(),
                refresh.token(), refresh.expiresAt(),
                UserResponse.from(user));
    }
}
