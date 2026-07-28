package com.debtpulse.common.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Cross-cutting logger for every service and controller method in every DebtPulse
 * microservice. Logs entry (with arguments), successful exit (with duration), and any
 * thrown exception. All output is written to the configured {@code logs/spring.log}.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerLayer() {}

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceLayer() {}

    @Around("controllerLayer() || serviceLayer()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName() + "()";
        long start = System.currentTimeMillis();

        if (log.isInfoEnabled()) {
            log.info("ENTER {} args={}", signature, Arrays.toString(joinPoint.getArgs()));
        }
        try {
            Object result = joinPoint.proceed();
            long took = System.currentTimeMillis() - start;
            log.info("EXIT  {} ({} ms)", signature, took);
            return result;
        } catch (Throwable ex) {
            long took = System.currentTimeMillis() - start;
            log.error("ERROR {} ({} ms): {} - {}", signature, took,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }
}
