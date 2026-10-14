package com.careflow.serviceops.service;

import com.careflow.serviceops.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SlaStatusEvaluatorTest {

    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-10-09T10:00:00Z");
    private static final OffsetDateTime RESPONSE_RISK = T0.plusMinutes(45);
    private static final OffsetDateTime RESPONSE_DUE = T0.plusMinutes(60);
    private static final OffsetDateTime RESOLUTION_RISK = T0.plusMinutes(180);
    private static final OffsetDateTime RESOLUTION_DUE = T0.plusMinutes(240);

    private WorkOrder workOrder() {
        WorkOrder workOrder = new WorkOrder(UUID.randomUUID(), "WO-TEST", "t", "d", Priority.HIGH,
                mock(ServiceSite.class), null, RESOLUTION_DUE);
        workOrder.applySla(new SlaPlan(null, "p", "PRIORITY", 60, 240, false, "UTC",
                RESPONSE_DUE, RESPONSE_RISK, RESOLUTION_DUE, RESOLUTION_RISK));
        return workOrder;
    }

    @Test
    void onTrackBeforeTheAtRiskThreshold() {
        WorkOrder workOrder = workOrder();
        assertThat(SlaStatusEvaluator.response(workOrder, T0.plusMinutes(10))).isEqualTo(SlaState.ON_TRACK);
        assertThat(SlaStatusEvaluator.resolution(workOrder, T0.plusMinutes(10))).isEqualTo(SlaState.ON_TRACK);
    }

    @Test
    void atRiskExactlyAtTheThreshold() {
        assertThat(SlaStatusEvaluator.response(workOrder(), RESPONSE_RISK)).isEqualTo(SlaState.AT_RISK);
        assertThat(SlaStatusEvaluator.response(workOrder(), RESPONSE_RISK.minusSeconds(1))).isEqualTo(SlaState.ON_TRACK);
    }

    @Test
    void notBreachedExactlyAtTheDeadlineButBreachedOneSecondLater() {
        assertThat(SlaStatusEvaluator.response(workOrder(), RESPONSE_DUE)).isEqualTo(SlaState.AT_RISK);
        assertThat(SlaStatusEvaluator.response(workOrder(), RESPONSE_DUE.plusSeconds(1))).isEqualTo(SlaState.BREACHED);
        assertThat(SlaStatusEvaluator.resolution(workOrder(), RESOLUTION_DUE)).isEqualTo(SlaState.AT_RISK);
        assertThat(SlaStatusEvaluator.resolution(workOrder(), RESOLUTION_DUE.plusSeconds(1))).isEqualTo(SlaState.BREACHED);
    }

    @Test
    void responseStopsCountingOnceWorkIsPickedUp() {
        WorkOrder workOrder = workOrder();
        ReflectionTestUtils.setField(workOrder, "firstResponseAt", T0.plusMinutes(30));
        // long after the response deadline, but it was answered in time
        assertThat(SlaStatusEvaluator.response(workOrder, T0.plusDays(2))).isEqualTo(SlaState.MET);
    }

    @Test
    void lateResponseIsRecordedAsMetLate() {
        WorkOrder workOrder = workOrder();
        ReflectionTestUtils.setField(workOrder, "firstResponseAt", RESPONSE_DUE.plusMinutes(5));
        assertThat(SlaStatusEvaluator.response(workOrder, T0.plusDays(2))).isEqualTo(SlaState.MET_LATE);
    }

    @Test
    void changingStatusPastNewRecordsTheFirstResponseOnlyOnce() {
        WorkOrder workOrder = workOrder();
        assertThat(workOrder.getFirstResponseAt()).isNull();
        workOrder.changeStatus(WorkOrderStatus.ASSIGNED);
        OffsetDateTime first = workOrder.getFirstResponseAt();
        assertThat(first).isNotNull();
        workOrder.changeStatus(WorkOrderStatus.IN_PROGRESS);
        assertThat(workOrder.getFirstResponseAt()).isEqualTo(first);
    }

    @Test
    void resolvedWorkIsMetOrMetLateAgainstTheResolutionDeadline() {
        WorkOrder onTime = workOrder();
        onTime.changeStatus(WorkOrderStatus.RESOLVED);
        ReflectionTestUtils.setField(onTime, "resolvedAt", RESOLUTION_DUE.minusMinutes(1));
        assertThat(SlaStatusEvaluator.resolution(onTime, T0.plusDays(5))).isEqualTo(SlaState.MET);

        WorkOrder late = workOrder();
        late.changeStatus(WorkOrderStatus.RESOLVED);
        ReflectionTestUtils.setField(late, "resolvedAt", RESOLUTION_DUE.plusMinutes(1));
        assertThat(SlaStatusEvaluator.resolution(late, T0.plusDays(5))).isEqualTo(SlaState.MET_LATE);
    }

    @Test
    void cancelledWorkIsNotTracked() {
        WorkOrder workOrder = workOrder();
        workOrder.changeStatus(WorkOrderStatus.CANCELLED);
        assertThat(SlaStatusEvaluator.response(workOrder, T0.plusDays(9))).isEqualTo(SlaState.NOT_TRACKED);
        assertThat(SlaStatusEvaluator.resolution(workOrder, T0.plusDays(9))).isEqualTo(SlaState.NOT_TRACKED);
    }

    @Test
    void legacyWorkOrdersWithoutAPolicySnapshotStillGetAResolutionState() {
        WorkOrder legacy = new WorkOrder(UUID.randomUUID(), "WO-OLD", "t", "d", Priority.LOW,
                mock(ServiceSite.class), null, RESOLUTION_DUE);
        assertThat(SlaStatusEvaluator.response(legacy, T0)).isEqualTo(SlaState.NOT_TRACKED);
        assertThat(SlaStatusEvaluator.resolution(legacy, T0)).isEqualTo(SlaState.ON_TRACK);
        assertThat(SlaStatusEvaluator.resolution(legacy, RESOLUTION_DUE.plusMinutes(1))).isEqualTo(SlaState.BREACHED);
    }

    @Test
    void overallStateIsTheWorstOfTheTwo() {
        assertThat(SlaStatusEvaluator.overall(SlaState.MET, SlaState.BREACHED)).isEqualTo(SlaState.BREACHED);
        assertThat(SlaStatusEvaluator.overall(SlaState.AT_RISK, SlaState.ON_TRACK)).isEqualTo(SlaState.AT_RISK);
        assertThat(SlaStatusEvaluator.overall(SlaState.MET, SlaState.ON_TRACK)).isEqualTo(SlaState.ON_TRACK);
        assertThat(SlaStatusEvaluator.overall(SlaState.MET_LATE, SlaState.MET)).isEqualTo(SlaState.MET_LATE);
        assertThat(SlaStatusEvaluator.overall(SlaState.NOT_TRACKED, SlaState.NOT_TRACKED)).isEqualTo(SlaState.NOT_TRACKED);
    }
}