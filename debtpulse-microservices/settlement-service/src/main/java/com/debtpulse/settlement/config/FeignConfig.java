package com.debtpulse.settlement.config;

import com.debtpulse.common.config.FeignClientInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Registers the shared Feign interceptor that propagates identity headers downstream. */
@Configuration
@Import(FeignClientInterceptor.class)
public class FeignConfig {
}
