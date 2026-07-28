package com.debtpulse.gateway.security;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * The single security choke point for the whole system.
 *
 * <p>Runs on every request routed by the gateway. For protected paths it requires a valid
 * {@code Authorization: Bearer <jwt>} header, validates the signature/expiry, and then
 * injects the authenticated identity as trusted headers that downstream services read
 * (via their RoleBasedHeaderFilter) to rebuild the Spring Security context:</p>
 * <ul>
 *   <li>{@code X-Auth-UserId}</li>
 *   <li>{@code X-Auth-Role}</li>
 *   <li>{@code X-Auth-BranchId}</li>
 *   <li>{@code X-Auth-Name}</li>
 * </ul>
 * Open paths (login, password reset, swagger, actuator) bypass validation.
 */
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGlobalFilter.class);

    public static final String HDR_USER_ID = "X-Auth-UserId";
    public static final String HDR_ROLE = "X-Auth-Role";
    public static final String HDR_BRANCH = "X-Auth-BranchId";
    public static final String HDR_NAME = "X-Auth-Name";

    private final JwtValidator jwtValidator;
    private final List<String> openPaths;

    public JwtAuthenticationGlobalFilter(JwtValidator jwtValidator,
                                         @Value("${gateway.security.open-paths}") String openPathsCsv) {
        this.jwtValidator = jwtValidator;
        this.openPaths = Arrays.stream(openPathsCsv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Strip any spoofed identity headers arriving from outside — only the gateway may set them.
        ServerHttpRequest scrubbed = request.mutate()
                .headers(h -> {
                    h.remove(HDR_USER_ID);
                    h.remove(HDR_ROLE);
                    h.remove(HDR_BRANCH);
                    h.remove(HDR_NAME);
                })
                .build();
        exchange = exchange.mutate().request(scrubbed).build();

        // Never authenticate a CORS preflight: it carries no Authorization header by design.
        // (Spring Cloud Gateway's globalcors normally answers preflights before filters run;
        // this guard guarantees a preflight can never be rejected with 401/403 by this filter.)
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        if (isOpenPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtValidator.parse(token);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            String branchId = claims.get("branchId", String.class);
            String name = claims.get("name", String.class);

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header(HDR_USER_ID, safe(userId))
                    .header(HDR_ROLE, safe(role))
                    .header(HDR_BRANCH, safe(branchId))
                    .header(HDR_NAME, safe(name))
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            log.warn("JWT validation failed for {}: {}", path, e.getMessage());
            return unauthorized(exchange, "Invalid or expired JWT token");
        }
    }

    private boolean isOpenPath(String path) {
        return openPaths.stream().anyMatch(path::startsWith);
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        byte[] bytes = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        // Run before routing so identity headers are attached before the request leaves the gateway.
        return -1;
    }
}
