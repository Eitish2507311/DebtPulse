package com.debtpulse.legal.config;

/**
 * Imports the shared {@link com.debtpulse.common.config.FeignClientInterceptor} so outbound
 * Feign calls propagate the caller's identity headers to downstream services.
 */
@org.springframework.context.annotation.Configuration
@org.springframework.context.annotation.Import(com.debtpulse.common.config.FeignClientInterceptor.class)
public class FeignConfig {}
