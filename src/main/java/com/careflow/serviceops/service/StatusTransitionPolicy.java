package com.careflow.serviceops.service;

import com.careflow.serviceops.domain.WorkOrderStatus;
import com.careflow.serviceops.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class StatusTransitionPolicy {

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> ALLOWED = Map.of(
            WorkOrderStatus.NEW, Set.of(WorkOrderStatus.ASSIGNED, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.ASSIGNED, Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.ON_HOLD, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.IN_PROGRESS, Set.of(WorkOrderStatus.ON_HOLD, WorkOrderStatus.RESOLVED),
            WorkOrderStatus.ON_HOLD, Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.RESOLVED, Set.of(),
            WorkOrderStatus.CANCELLED, Set.of()
    );

    public void verify(WorkOrderStatus current, WorkOrderStatus target) {
        if (current == target) {
            throw new BusinessRuleException("Work order is already in status " + target + ".");
        }
        if (!ALLOWED.getOrDefault(current, Set.of()).contains(target)) {
            throw new BusinessRuleException("Transition from " + current + " to " + target + " is not allowed.");
        }
    }
}
