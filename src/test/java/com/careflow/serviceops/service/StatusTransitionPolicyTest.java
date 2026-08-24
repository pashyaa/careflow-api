package com.careflow.serviceops.service;

import com.careflow.serviceops.domain.WorkOrderStatus;
import com.careflow.serviceops.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusTransitionPolicyTest {

    private final StatusTransitionPolicy policy = new StatusTransitionPolicy();

    @Test
    void allowsExpectedOperationalTransition() {
        assertThatCode(() -> policy.verify(WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsSkippingDirectlyFromNewToResolved() {
        assertThatThrownBy(() -> policy.verify(WorkOrderStatus.NEW, WorkOrderStatus.RESOLVED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void rejectsTransitionToCurrentStatus() {
        assertThatThrownBy(() -> policy.verify(WorkOrderStatus.ON_HOLD, WorkOrderStatus.ON_HOLD))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already");
    }
}

