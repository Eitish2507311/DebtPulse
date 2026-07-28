package com.debtpulse.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Convenience accessors for the authenticated principal within a service.
 * The principal name is the userId (the JWT subject), populated by
 * {@link RoleBasedHeaderFilter}.
 */
public final class AuthContext {

    private AuthContext() {}

    /** Current user's id (JWT subject), or {@code null} if unauthenticated. */
    public static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    /** Current user's role without the {@code ROLE_} prefix, or {@code null}. */
    public static String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().isEmpty()) return null;
        String authority = auth.getAuthorities().iterator().next().getAuthority();
        return authority.startsWith("ROLE_") ? authority.substring(5) : authority;
    }

    public static boolean hasRole(String role) {
        return role != null && role.equals(currentRole());
    }

    public static boolean isManagerOrAdmin() {
        String role = currentRole();
        return "PORTFOLIO_MANAGER".equals(role) || "ADMIN".equals(role);
    }
}
