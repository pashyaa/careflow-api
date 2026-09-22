package com.careflow.serviceops.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// changedBy was intentionally removed: the acting user is derived server-side from the
// authenticated principal (see CurrentActorResolver), never trusted from client input.
public record AssignTechnicianRequest(
        @NotNull UUID technicianId
) {
}