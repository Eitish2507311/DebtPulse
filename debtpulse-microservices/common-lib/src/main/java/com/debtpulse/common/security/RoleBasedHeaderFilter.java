package com.debtpulse.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Rebuilds the Spring Security context inside a downstream microservice from the trusted
 * identity headers set by the API Gateway (after it validated the JWT).
 *
 * <p>The gateway is the only component that validates the token; each service trusts the
 * {@code X-Auth-*} headers and turns the role into a {@code ROLE_<role>} authority so that
 * standard {@code @PreAuthorize("hasRole('X')")} / {@code hasAnyRole(...)} checks work exactly
 * as they did in the monolith.</p>
 */
public class RoleBasedHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String userId = request.getHeader(SecurityHeaders.USER_ID);
        String role = request.getHeader(SecurityHeaders.ROLE);

        if (userId != null && !userId.isBlank() && role != null && !role.isBlank()) {
            var authority = new SimpleGrantedAuthority("ROLE_" + role);
            var authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(request, response);
    }
}
