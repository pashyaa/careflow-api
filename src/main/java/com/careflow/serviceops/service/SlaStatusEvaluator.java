package com.careflow.serviceops.service;

import com.careflow.serviceops.domain.SlaState;
import com.careflow.serviceops.domain.WorkOrder;
import com.careflow.serviceops.domain.WorkOrderStatus;

import java.time.OffsetDateTime;

/**
 * Derives the SLA state of a work order at a given moment.
 * Boundaries: exactly AT a deadline is still on time (breach = strictly after);
 * exactly AT the at-risk threshold already counts as at risk.
 */
public final class SlaStatusEvaluator {

    private SlaStatusEvaluator() {
    }

    public static SlaState response(WorkOrder workOrder, OffsetDateTime now) {
        OffsetDateTime due = workOrder.getResponseDueAt();
        if (due == null) return SlaState.NOT_TRACKED;                       // legacy work order
        if (workOrder.getFirstResponseAt() != null) {
            return workOrder.getFirstResponseAt().isAfter(due) ? SlaState.MET_LATE : SlaState.MET;
        }
        if (workOrder.getStatus() == WorkOrderStatus.CANCELLED) return SlaState.NOT_TRACKED;
        return running(now, due, workOrder.getResponseAtRiskAt());
    }

    public static SlaState resolution(WorkOrder workOrder, OffsetDateTime now) {
        OffsetDateTime due = workOrder.getTargetResolutionAt();
        return switch (workOrder.getStatus()) {
            case CANCELLED -> SlaState.NOT_TRACKED;
            case RESOLVED -> {
                OffsetDateTime done = workOrder.getResolvedAt() != null ? workOrder.getResolvedAt() : now;
                yield done.isAfter(due) ? SlaState.MET_LATE : SlaState.MET;
            }
            default -> running(now, due, workOrder.getResolutionAtRiskAt());
        };
    }

    /** The worst of the two, so one number/badge summarises the work order. */
    public static SlaState overall(SlaState response, SlaState resolution) {
        if (response == SlaState.BREACHED || resolution == SlaState.BREACHED) return SlaState.BREACHED;
        if (response == SlaState.AT_RISK || resolution == SlaState.AT_RISK) return SlaState.AT_RISK;
        if (response == SlaState.ON_TRACK || resolution == SlaState.ON_TRACK) return SlaState.ON_TRACK;
        if (response == SlaState.MET_LATE || resolution == SlaState.MET_LATE) return SlaState.MET_LATE;
        if (response == SlaState.MET || resolution == SlaState.MET) return SlaState.MET;
        return SlaState.NOT_TRACKED;
    }

    private static SlaState running(OffsetDateTime now, OffsetDateTime due, OffsetDateTime atRisk) {
        if (now.isAfter(due)) return SlaState.BREACHED;
        if (atRisk != null && !now.isBefore(atRisk)) return SlaState.AT_RISK;
        return SlaState.ON_TRACK;
    }
}