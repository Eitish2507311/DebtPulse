package com.debtpulse.common.observability;

/** Header + MDC key names for request correlation/tracing across services. */
public final class CorrelationHeaders {

    private CorrelationHeaders() {}

    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String REQUEST_ID = "X-Request-Id";

    /** MDC keys (used in the log pattern and by the audit framework). */
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_REQUEST_ID = "requestId";
}
