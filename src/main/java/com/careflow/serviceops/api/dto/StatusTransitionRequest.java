package com.careflow.serviceops.api.dto;

import com.careflow.serviceops.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// changedBy was intentionally removed: the acting user is derived server-side from the
// authenticated principal (see CurrentActorResolver), never trusted from client input.
public record StatusTransitionRequest(
        @NotNull WorkOrderStatus status,
        @Size(max = 500) String note
) {
}