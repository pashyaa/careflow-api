package com.careflow.serviceops.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Immutable result of selecting a policy and computing its deadlines for one work order. */
public record SlaPlan(
        UUID policyId,
        String policyName,
        String policyScope,
        int responseMinutes,
        int resolutionMinutes,
        boolean businessHoursOnly,
        String timeZone,
        OffsetDateTime responseDueAt,
        OffsetDateTime responseAtRiskAt,
        OffsetDateTime resolutionDueAt,
        OffsetDateTime resolutionAtRiskAt
) {
}