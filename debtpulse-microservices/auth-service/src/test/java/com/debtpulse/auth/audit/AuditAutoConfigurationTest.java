package com.debtpulse.auth.audit;

import com.debtpulse.common.audit.AuditAutoConfiguration;
import com.debtpulse.common.audit.AuditPublisher;
import com.debtpulse.common.audit.LoggingAuditPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the audit auto-configuration wires correctly in a real (minimal) Spring context — the
 * class of failure that unit tests miss and that broke startup previously. Uses ApplicationContextRunner
 * so it needs no database or web server (runs fully offline).
 */
class AuditAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class));

    @Test
    void providesDefaultLoggingPublisher_whenNoneDefined() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(AuditPublisher.class)
                .getBean(AuditPublisher.class).isInstanceOf(LoggingAuditPublisher.class));
    }

    @Test
    void backsOff_whenServiceProvidesItsOwnPublisher() {
        runner.withUserConfiguration(CustomPublisherConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(AuditPublisher.class);
            assertThat(ctx.getBean(AuditPublisher.class)).isNotInstanceOf(LoggingAuditPublisher.class);
        });
    }

    @Configuration
    static class CustomPublisherConfig {
        @Bean
        AuditPublisher customPublisher() {
            return event -> { /* e.g. persist to DB / Kafka */ };
        }
    }
}
