package com.debtpulse.common.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Convenience accessors for the authenticated principal within a service.
 * The principal name is the userId (the JWT subject), populated by
 * {@link RoleBasedHeaderFilter}.
 */
public final class AuthContext {

    private AuthContext() {}

    /**
     * Current user's id (JWT subject), or {@code null} if there is no real authenticated principal.
     * On open/permitAll paths (login, logout, refresh, forgot-password) Spring Security still
     * installs an {@link AnonymousAuthenticationToken} whose name is the literal string
     * {@code "anonymousUser"} — we treat that as unauthenticated and return {@code null}.
     */
    public static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isUnauthenticated(auth)) return null;
        return auth.getName();
    }

    /** Current user's role without the {@code ROLE_} prefix, or {@code null} if unauthenticated. */
    public static String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isUnauthenticated(auth) || auth.getAuthorities().isEmpty()) return null;
        String authority = auth.getAuthorities().iterator().next().getAuthority();
        if ("ROLE_ANONYMOUS".equals(authority)) return null;
        return authority.startsWith("ROLE_") ? authority.substring(5) : authority;
    }

    /** True when there is no real authenticated principal (missing, not authenticated, or anonymous). */
    private static boolean isUnauthenticated(Authentication auth) {
        return auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken;
    }

    public static boolean hasRole(String role) {
        return role != null && role.equals(currentRole());
    }

    public static boolean isManagerOrAdmin() {
        String role = currentRole();
        return "PORTFOLIO_MANAGER".equals(role) || "ADMIN".equals(role);
    }
}
