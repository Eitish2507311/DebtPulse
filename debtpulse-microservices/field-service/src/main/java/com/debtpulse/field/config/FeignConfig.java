package com.debtpulse.field.config;

/**
 * Propagates the caller's identity headers onto outbound Feign calls so downstream
 * services (account, auth, notification) see the same authenticated principal.
 */
@org.springframework.context.annotation.Configuration
@org.springframework.context.annotation.Import(com.debtpulse.common.config.FeignClientInterceptor.class)
public class FeignConfig {}
