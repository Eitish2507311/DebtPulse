package com.debtpulse.common.audit;

import com.debtpulse.common.observability.CorrelationHeaders;
import com.debtpulse.common.security.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;

/**
 * Turns {@link Auditable} into a structured {@link AuditEvent}. Runs after the target method so it
 * can see the return value (for id resolution) and the outcome, capturing the actor (from the
 * security context), the request-tracing ids (from MDC) and the client IP. Auditing never breaks the
 * business operation — any failure to build/publish the event is swallowed with a warning.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditPublisher publisher;
    private final String serviceName;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer paramNames = new DefaultParameterNameDiscoverer();

    public AuditAspect(AuditPublisher publisher,
                       @Value("${spring.application.name:unknown}") String serviceName) {
        this.publisher = publisher;
        this.serviceName = serviceName;
    }

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void onSuccess(JoinPoint jp, Auditable auditable, Object result) {
        record(jp, auditable, result, "SUCCESS", null);
    }

    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "ex")
    public void onFailure(JoinPoint jp, Auditable auditable, Throwable ex) {
        record(jp, auditable, null, "FAILURE", ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }

    private void record(JoinPoint jp, Auditable auditable, Object result, String outcome, String detail) {
        try {
            AuditEvent event = new AuditEvent(
                    MDC.get(CorrelationHeaders.MDC_CORRELATION_ID),
                    MDC.get(CorrelationHeaders.MDC_REQUEST_ID),
                    AuthContext.currentUserId(),
                    AuthContext.currentRole(),
                    serviceName,
                    auditable.entity(),
                    resolveEntityId(jp, auditable, result),
                    auditable.action(),
                    clientIp(),
                    OffsetDateTime.now(),
                    outcome,
                    detail);
            publisher.publish(event);
            // Tell HttpAuditAspect this request is already audited, so it won't add a duplicate row.
            AuditContext.markRecorded();
        } catch (Exception e) {
            log.warn("Audit publish failed for action={}: {}", auditable.action(), e.getMessage());
        }
    }

    private String resolveEntityId(JoinPoint jp, Auditable auditable, Object result) {
        String expr = auditable.entityId();
        if (expr == null || expr.isBlank()) {
            return null;
        }
        try {
            Method method = ((MethodSignature) jp.getSignature()).getMethod();
            EvaluationContext ctx = new MethodBasedEvaluationContext(
                    jp.getTarget(), method, jp.getArgs(), paramNames);
            ((MethodBasedEvaluationContext) ctx).setVariable("result", result);
            Object value = parser.parseExpression(expr).getValue(ctx);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            // A null result (e.g. on @AfterThrowing) or a mistyped expression must never
            // abort the audit — the event is still recorded, just without an entity id.
            log.debug("Could not resolve audit entityId from '{}': {}", expr, e.getMessage());
            return null;
        }
    }

    private String clientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
