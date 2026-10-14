package com.careflow.serviceops.api.dto;

import com.careflow.serviceops.domain.SlaPlan;

import java.time.OffsetDateTime;

public record SlaPreviewResponse(
        String policyName,
        String policyScope,
        int responseMinutes,
        int resolutionMinutes,
        boolean businessHoursOnly,
        String timeZone,
        OffsetDateTime responseDueAt,
        OffsetDateTime resolutionDueAt
) {
    public static SlaPreviewResponse from(SlaPlan plan) {
        return new SlaPreviewResponse(plan.policyName(), plan.policyScope(), plan.responseMinutes(),
                plan.resolutionMinutes(), plan.businessHoursOnly(), plan.timeZone(),
                plan.responseDueAt(), plan.resolutionDueAt());
    }
}