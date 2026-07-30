package com.debtpulse.legal.service;

import com.debtpulse.common.enums.CaseStatus;
import com.debtpulse.common.enums.OrderStatus;
import com.debtpulse.legal.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LegalStatusPolicyTest {

    @Test
    void case_allowsLawfulProgression() {
        assertThatCode(() -> LegalStatusPolicy.assertCaseTransition(CaseStatus.FILED, CaseStatus.HEARING_SCHEDULED))
                .doesNotThrowAnyException();
        assertThatCode(() -> LegalStatusPolicy.assertCaseTransition(CaseStatus.HEARING_SCHEDULED, CaseStatus.DECREED))
                .doesNotThrowAnyException();
        assertThatCode(() -> LegalStatusPolicy.assertCaseTransition(CaseStatus.DECREED, CaseStatus.SETTLED))
                .doesNotThrowAnyException();
        // re-setting the same status is a no-op edit, always allowed
        assertThatCode(() -> LegalStatusPolicy.assertCaseTransition(CaseStatus.FILED, CaseStatus.FILED))
                .doesNotThrowAnyException();
    }

    @Test
    void case_rejectsIllegalJumps() {
        // cannot skip straight to a terminal/decreed state from FILED
        assertThatThrownBy(() -> LegalStatusPolicy.assertCaseTransition(CaseStatus.FILED, CaseStatus.SETTLED))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> LegalStatusPolicy.assertCaseTransition(CaseStatus.FILED, CaseStatus.DECREED))
                .isInstanceOf(BusinessRuleException.class);
        // terminal states accept no further change
        assertThatThrownBy(() -> LegalStatusPolicy.assertCaseTransition(CaseStatus.SETTLED, CaseStatus.HEARING_SCHEDULED))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void case_dismissal_isReachableFromHearingAndTerminal() {
        // a case can be dismissed at a scheduled hearing
        assertThatCode(() -> LegalStatusPolicy.assertCaseTransition(CaseStatus.HEARING_SCHEDULED, CaseStatus.DISMISSED))
                .doesNotThrowAnyException();
        // but not without one, and dismissal is terminal
        assertThatThrownBy(() -> LegalStatusPolicy.assertCaseTransition(CaseStatus.FILED, CaseStatus.DISMISSED))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> LegalStatusPolicy.assertCaseTransition(CaseStatus.DISMISSED, CaseStatus.PENDING))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void order_allowsLawfulProgression_andRejectsJumps() {
        assertThatCode(() -> LegalStatusPolicy.assertOrderTransition(OrderStatus.ISSUED, OrderStatus.IN_EXECUTION))
                .doesNotThrowAnyException();
        assertThatCode(() -> LegalStatusPolicy.assertOrderTransition(OrderStatus.IN_EXECUTION, OrderStatus.EXECUTED))
                .doesNotThrowAnyException();
        // cannot jump ISSUED -> EXECUTED without executing first
        assertThatThrownBy(() -> LegalStatusPolicy.assertOrderTransition(OrderStatus.ISSUED, OrderStatus.EXECUTED))
                .isInstanceOf(BusinessRuleException.class);
        // EXECUTED is terminal
        assertThatThrownBy(() -> LegalStatusPolicy.assertOrderTransition(OrderStatus.EXECUTED, OrderStatus.VACATED))
                .isInstanceOf(BusinessRuleException.class);
    }
}
