package com.careflow.serviceops.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AssignTechnicianRequest(
        @NotNull UUID technicianId,
        @NotBlank @Size(max = 120) String changedBy
) {
}

