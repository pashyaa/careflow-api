package com.careflow.serviceops.api.dto;

import com.careflow.serviceops.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusTransitionRequest(
        @NotNull WorkOrderStatus status,
        @Size(max = 500) String note,
        @NotBlank @Size(max = 120) String changedBy
) {
}

