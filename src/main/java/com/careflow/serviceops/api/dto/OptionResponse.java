package com.careflow.serviceops.api.dto;

import java.util.UUID;

public record OptionResponse(UUID id, String code, String label, String secondaryText) {
}

