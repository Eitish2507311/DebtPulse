package com.debtpulse.contact.config;

/**
 * Imports the shared {@link com.debtpulse.common.config.FeignClientInterceptor} so every
 * outbound Feign call from contact-service carries the caller's identity headers.
 */
@org.springframework.context.annotation.Configuration
@org.springframework.context.annotation.Import(com.debtpulse.common.config.FeignClientInterceptor.class)
public class FeignConfig {}
