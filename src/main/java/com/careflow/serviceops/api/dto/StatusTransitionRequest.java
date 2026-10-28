package com.careflow.serviceops.api.dto;

import com.careflow.serviceops.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// changedBy was intentionally removed: the acting user is derived server-side from the
// authenticated principal (see CurrentActorResolver), never trusted from client input.
// BUG-305: reasonCode is required (with a meaningful note) when status is RESOLVED or CANCELLED;
// that rule is enforced in WorkOrderService because it depends on configured reason codes.
public record StatusTransitionRequest(
        @NotNull WorkOrderStatus status,
        @Size(max = 500) String note,
        @Size(max = 40) String reasonCode
) {
}