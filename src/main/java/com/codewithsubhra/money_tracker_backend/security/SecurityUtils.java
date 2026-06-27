package com.codewithsubhra.money_tracker_backend.security;

import com.codewithsubhra.money_tracker_backend.common.exception.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Convenience accessors for the currently authenticated principal. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal principal) {
            return principal;
        }
        throw new UnauthorizedException("Authentication required");
    }

    public static UUID currentUserId() {
        return currentPrincipal().userId();
    }
}
