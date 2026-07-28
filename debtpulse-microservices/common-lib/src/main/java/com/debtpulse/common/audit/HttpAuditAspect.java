package com.debtpulse.common.audit;

import com.debtpulse.common.observability.CorrelationHeaders;
import com.debtpulse.common.security.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.OffsetDateTime;

/**
 * Blanket, zero-touch audit for EVERY state-changing REST endpoint across every service. Any method
 * in a {@code @RestController} annotated with {@link PostMapping}/{@link PutMapping}/{@link PatchMapping}/
 * {@link DeleteMapping} is audited automatically on success, with the action/entity/entityId derived
 * from the controller and request.
 *
 * <p>De-duplication: a method that already audits itself — an explicit {@code authClient.audit(...)}
 * call, or the {@link AuditAspect} via {@link Auditable} — sets {@link AuditContext}, and this aspect
 * then skips it. An explicit {@link Auditable} or {@link SkipAutoAudit} on the method is also honoured
 * (manual annotation always wins). Auditing never breaks the business operation: publish failures are
 * swallowed with a warning.</p>
 */
@Aspect
@Component
@Order(20) // run outside CorrelationIdFilter/security; wraps the whole controller invocation
public class HttpAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(HttpAuditAspect.class);

    private final AuditPublisher publisher;
    private final String serviceName;

    public HttpAuditAspect(AuditPublisher publisher,
                           @Value("${spring.application.name:unknown}") String serviceName) {
        this.publisher = publisher;
        this.serviceName = serviceName;
    }

    @Around("(@within(org.springframework.web.bind.annotation.RestController) "
            + "|| @within(org.springframework.stereotype.Controller)) "
            + "&& (@annotation(org.springframework.web.bind.annotation.PostMapping) "
            + "|| @annotation(org.springframework.web.bind.annotation.PutMapping) "
            + "|| @annotation(org.springframework.web.bind.annotation.PatchMapping) "
            + "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public Object audit(ProceedingJoinPoint jp) throws Throwable {
        AuditContext.reset(); // clean slate for this request (pooled threads are reused)
        Object result;
        try {
            result = jp.proceed();
        } finally {
            // failures propagate; we only auto-audit successful mutations below
        }
        try {
            Method method = ((MethodSignature) jp.getSignature()).getMethod();
            boolean manualOverride = method.isAnnotationPresent(Auditable.class)
                    || method.isAnnotationPresent(SkipAutoAudit.class)
                    || AuditContext.wasRecorded();
            if (!manualOverride) {
                publisher.publish(buildEvent(jp, method, result));
            }
        } catch (Exception e) {
            log.warn("Automatic audit failed for {}: {}",
                    jp.getSignature().toShortString(), e.getMessage());
        } finally {
            AuditContext.reset();
        }
        return result;
    }

    private AuditEvent buildEvent(ProceedingJoinPoint jp, Method method, Object result) {
        String resource = resourceName(jp.getTarget().getClass());
        String action = resource.toUpperCase() + "_" + verb(method);
        return new AuditEvent(
                MDC.get(CorrelationHeaders.MDC_CORRELATION_ID),
                MDC.get(CorrelationHeaders.MDC_REQUEST_ID),
                AuthContext.currentUserId(),
                AuthContext.currentRole(),
                serviceName,
                resource,
                resolveEntityId(method, jp.getArgs(), result),
                action,
                clientIp(),
                OffsetDateTime.now(),
                "SUCCESS",
                null);
    }

    /** e.g. AccountController → "Account", LegalCaseController → "LegalCase". */
    private String resourceName(Class<?> controller) {
        String name = controller.getSimpleName();
        int idx = name.indexOf("Controller");
        return idx > 0 ? name.substring(0, idx) : name;
    }

    private String verb(Method method) {
        if (method.isAnnotationPresent(PostMapping.class)) return "CREATE";
        if (method.isAnnotationPresent(PutMapping.class)) return "UPDATE";
        if (method.isAnnotationPresent(PatchMapping.class)) return "UPDATE";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        return "ACTION";
    }

    /**
     * Best-effort entity id: a {@code @PathVariable} named id/xxxId first, otherwise the return
     * value's most likely id getter (getXxxId()/xxxId()) via reflection, else null.
     */
    private String resolveEntityId(Method method, Object[] args, Object result) {
        String fromPath = pathVariableId(method, args);
        if (fromPath != null) return fromPath;
        return idFromResult(result);
    }

    private String pathVariableId(Method method, Object[] args) {
        Parameter[] params = method.getParameters();
        String firstIdLike = null;
        for (int i = 0; i < params.length && i < args.length; i++) {
            PathVariable pv = params[i].getAnnotation(PathVariable.class);
            if (pv == null || args[i] == null) continue;
            String name = !pv.value().isBlank() ? pv.value()
                    : (!pv.name().isBlank() ? pv.name() : params[i].getName());
            if ("id".equalsIgnoreCase(name)) return args[i].toString();
            if (firstIdLike == null && name.toLowerCase().endsWith("id")) firstIdLike = args[i].toString();
        }
        return firstIdLike;
    }

    private String idFromResult(Object result) {
        if (result == null) return null;
        Object body = unwrapBody(result);
        if (body == null || isSimple(body)) return null;
        // Prefer getXxxId()/getId(); fall back to record-style xxxId()/id() accessors.
        String[] candidates = {"getId", "getEntityId"};
        for (String getter : candidates) {
            String v = invokeString(body, getter);
            if (v != null) return v;
        }
        for (Method m : body.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            String n = m.getName();
            boolean idGetter = (n.startsWith("get") && n.endsWith("Id") && n.length() > 5)
                    || (n.endsWith("Id") && n.length() > 2 && Character.isLowerCase(n.charAt(0)));
            if (idGetter) {
                String v = invokeString(body, n);
                if (v != null) return v;
            }
        }
        return null;
    }

    private Object unwrapBody(Object result) {
        if (result instanceof org.springframework.http.ResponseEntity<?> re) {
            return re.getBody();
        }
        return result;
    }

    private boolean isSimple(Object o) {
        return o instanceof CharSequence || o instanceof Number || o instanceof Boolean
                || o instanceof java.util.Map || o instanceof java.util.Collection;
    }

    private String invokeString(Object target, String getter) {
        try {
            Method m = target.getClass().getMethod(getter);
            Object v = m.invoke(target);
            return v == null ? null : v.toString();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private String clientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
