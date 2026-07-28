package com.debtpulse.common.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the shared audit framework. Registers a default {@link AuditPublisher}
 * (structured logging) unless the importing service defines its own — the canonical Spring Boot
 * "sensible default, overridable" pattern.
 *
 * <p>Registered via {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports},
 * so it is processed AFTER the service's own beans — which is exactly why {@code @ConditionalOnMissingBean}
 * works here (unlike on a component-scanned {@code @Component}, where it evaluates against a half-built
 * registry). The {@link AuditAspect} itself stays a component-scanned bean in the services.</p>
 */
@AutoConfiguration
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditPublisher.class)
    public AuditPublisher loggingAuditPublisher() {
        return new LoggingAuditPublisher();
    }
}
