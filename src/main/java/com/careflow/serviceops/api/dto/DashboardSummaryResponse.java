package com.careflow.serviceops.api.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record DashboardSummaryResponse(
        long openWorkOrders,
        long overdueWorkOrders,
        long unassignedWorkOrders,
        long criticalOpenWorkOrders,
        Map<String, Long> statusBreakdown,
        OffsetDateTime generatedAt
) {
}

