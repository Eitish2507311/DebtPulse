package com.debtpulse.account.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Registers the shared {@link com.debtpulse.common.config.FeignClientInterceptor} so outbound
 * Feign calls propagate the caller's identity headers to downstream services.
 */
@Configuration
@Import(com.debtpulse.common.config.FeignClientInterceptor.class)
public class FeignConfig {
}
