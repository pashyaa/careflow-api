package com.careflow.serviceops.api.dto;

import com.careflow.serviceops.domain.Priority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** null siteId / category / priority = "applies to any". 129,600 minutes = 90 days. */
public record SlaPolicyRequest(
        @NotBlank @Size(max = 120) String name,
        UUID siteId,
        @Size(max = 80) String category,
        Priority priority,
        @NotNull @Min(1) @Max(129600) Integer responseMinutes,
        @NotNull @Min(1) @Max(129600) Integer resolutionMinutes,
        boolean businessHoursOnly,
        Boolean active
) {
}