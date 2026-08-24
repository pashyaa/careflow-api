package com.careflow.serviceops.api.dto;

import com.careflow.serviceops.domain.Priority;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateWorkOrderRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull Priority priority,
        @NotNull UUID siteId,
        UUID assetId,
        @NotNull @Future OffsetDateTime targetResolutionAt
) {
}

