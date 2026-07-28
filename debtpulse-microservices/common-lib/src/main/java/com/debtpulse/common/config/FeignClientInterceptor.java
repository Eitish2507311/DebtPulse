package com.debtpulse.common.config;

import com.debtpulse.common.observability.CorrelationHeaders;
import com.debtpulse.common.security.SecurityHeaders;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Propagates the caller's identity headers onto every outbound Feign request so that the
 * called service can rebuild the same security context (via RoleBasedHeaderFilter).
 * Without this, inter-service calls would arrive unauthenticated and be rejected.
 */
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // Propagate the correlation id so the whole call chain shares one trace (works for
        // scheduled jobs too — generate one if there's no active request/MDC value).
        String correlationId = MDC.get(CorrelationHeaders.MDC_CORRELATION_ID);
        template.header(CorrelationHeaders.CORRELATION_ID,
                (correlationId == null || correlationId.isBlank()) ? UUID.randomUUID().toString() : correlationId);

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            // A scheduled job (no inbound HTTP request) — call downstream as the SYSTEM principal
            // with ADMIN authority so cross-service maintenance calls are accepted.
            template.header(SecurityHeaders.USER_ID, "SYSTEM");
            template.header(SecurityHeaders.ROLE, "ADMIN");
            return;
        }
        HttpServletRequest request = attrs.getRequest();
        copy(template, request, SecurityHeaders.USER_ID);
        copy(template, request, SecurityHeaders.ROLE);
        copy(template, request, SecurityHeaders.BRANCH_ID);
        copy(template, request, SecurityHeaders.NAME);
    }

    private void copy(RequestTemplate template, HttpServletRequest request, String header) {
        String value = request.getHeader(header);
        if (value != null && !value.isBlank()) {
            template.header(header, value);
        }
    }
}
