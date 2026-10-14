package com.careflow.serviceops.api.dto;

import com.careflow.serviceops.domain.Priority;
import com.careflow.serviceops.domain.SlaPolicy;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SlaPolicyResponse(
        UUID id,
        String name,
        UUID siteId,
        String category,
        Priority priority,
        String scope,
        int specificity,
        int responseMinutes,
        int resolutionMinutes,
        boolean businessHoursOnly,
        boolean active,
        OffsetDateTime updatedAt
) {
    public static SlaPolicyResponse from(SlaPolicy policy) {
        return new SlaPolicyResponse(
                policy.getId(), policy.getName(), policy.getSiteId(), policy.getCategory(), policy.getPriority(),
                policy.scopeLabel(), policy.specificity(), policy.getResponseMinutes(), policy.getResolutionMinutes(),
                policy.isBusinessHoursOnly(), policy.isActive(), policy.getUpdatedAt());
    }
}