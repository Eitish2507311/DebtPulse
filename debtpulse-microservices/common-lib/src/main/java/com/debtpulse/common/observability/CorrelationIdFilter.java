package com.debtpulse.common.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Establishes request tracing for every inbound request. A correlation id (propagated from the
 * caller via {@code X-Correlation-Id}, or generated) ties together all logs and audit events for a
 * single end-to-end flow across services; a per-request id ({@code X-Request-Id}) identifies the
 * individual hop. Both are placed in the SLF4J MDC (so they appear in every log line) and echoed
 * back on the response. Runs first so downstream filters/handlers are already traced.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String correlationId = header(request, CorrelationHeaders.CORRELATION_ID, UUID.randomUUID().toString());
        String requestId = UUID.randomUUID().toString();
        try {
            MDC.put(CorrelationHeaders.MDC_CORRELATION_ID, correlationId);
            MDC.put(CorrelationHeaders.MDC_REQUEST_ID, requestId);
            response.setHeader(CorrelationHeaders.CORRELATION_ID, correlationId);
            response.setHeader(CorrelationHeaders.REQUEST_ID, requestId);
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationHeaders.MDC_CORRELATION_ID);
            MDC.remove(CorrelationHeaders.MDC_REQUEST_ID);
        }
    }

    private static String header(HttpServletRequest request, String name, String fallback) {
        String value = request.getHeader(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
