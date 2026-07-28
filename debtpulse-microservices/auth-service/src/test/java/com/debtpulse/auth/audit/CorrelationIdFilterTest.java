package com.debtpulse.auth.audit;

import com.debtpulse.common.observability.CorrelationHeaders;
import com.debtpulse.common.observability.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void propagatesIncomingCorrelationId_andClearsMdcAfter() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(CorrelationHeaders.CORRELATION_ID, "cid-123");
        MockHttpServletResponse res = new MockHttpServletResponse();

        String[] seenDuringChain = new String[1];
        filter.doFilter(req, res, (rq, rs) ->
                seenDuringChain[0] = MDC.get(CorrelationHeaders.MDC_CORRELATION_ID));

        assertThat(seenDuringChain[0]).isEqualTo("cid-123");                       // available inside the chain
        assertThat(res.getHeader(CorrelationHeaders.CORRELATION_ID)).isEqualTo("cid-123"); // echoed back
        assertThat(res.getHeader(CorrelationHeaders.REQUEST_ID)).isNotBlank();
        assertThat(MDC.get(CorrelationHeaders.MDC_CORRELATION_ID)).isNull();       // cleaned up after
    }

    @Test
    void generatesCorrelationId_whenAbsent() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        String[] seen = new String[1];
        filter.doFilter(req, res, (rq, rs) ->
                seen[0] = MDC.get(CorrelationHeaders.MDC_CORRELATION_ID));

        assertThat(seen[0]).isNotBlank();
        assertThat(res.getHeader(CorrelationHeaders.CORRELATION_ID)).isEqualTo(seen[0]);
    }
}
