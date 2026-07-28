package com.debtpulse.auth.audit;

import com.debtpulse.common.audit.AuditAspect;
import com.debtpulse.common.audit.AuditEvent;
import com.debtpulse.common.audit.AuditPublisher;
import com.debtpulse.common.audit.Auditable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Verifies the reusable audit aspect: event construction, SpEL id resolution, success & failure. */
class AuditAspectTest {

    /** Sample target whose methods carry @Auditable. */
    static class Sample {
        @Auditable(action = "CREATE", entity = "Widget", entityId = "#id")
        public String make(String id) { return "made-" + id; }

        @Auditable(action = "ECHO", entity = "Widget", entityId = "#result")
        public String echo(String id) { return id; }

        @Auditable(action = "REMOVE", entity = "Widget", entityId = "#id")
        public void boom(String id) { throw new IllegalStateException("kaboom"); }
    }

    private Sample proxyWith(AuditPublisher publisher) {
        AspectJProxyFactory factory = new AspectJProxyFactory(new Sample());
        factory.addAspect(new AuditAspect(publisher, "test-service"));
        return factory.getProxy();
    }

    @Test
    void publishesSuccessEvent_withSpelParamId() {
        AuditPublisher publisher = mock(AuditPublisher.class);
        proxyWith(publisher).make("A1");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(publisher).publish(captor.capture());
        AuditEvent e = captor.getValue();
        assertThat(e.action()).isEqualTo("CREATE");
        assertThat(e.entity()).isEqualTo("Widget");
        assertThat(e.entityId()).isEqualTo("A1");
        assertThat(e.outcome()).isEqualTo("SUCCESS");
        assertThat(e.service()).isEqualTo("test-service");
        assertThat(e.timestamp()).isNotNull();
    }

    @Test
    void resolvesResultVariable() {
        AuditPublisher publisher = mock(AuditPublisher.class);
        proxyWith(publisher).echo("X9");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().entityId()).isEqualTo("X9");
    }

    @Test
    void publishesFailureEvent_whenMethodThrows() {
        AuditPublisher publisher = mock(AuditPublisher.class);
        Sample proxy = proxyWith(publisher);

        assertThatThrownBy(() -> proxy.boom("A2")).isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(publisher).publish(captor.capture());
        AuditEvent e = captor.getValue();
        assertThat(e.outcome()).isEqualTo("FAILURE");
        assertThat(e.entityId()).isEqualTo("A2");
        assertThat(e.detail()).contains("kaboom");
    }
}
