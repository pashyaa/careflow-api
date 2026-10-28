package com.careflow.serviceops.domain;

public enum ReasonCategory {
    RESOLUTION,
    CANCELLATION;

    /** The category a target status requires, or null if the status needs no reason code. */
    public static ReasonCategory forStatus(WorkOrderStatus status) {
        return switch (status) {
            case RESOLVED -> RESOLUTION;
            case CANCELLED -> CANCELLATION;
            default -> null;
        };
    }
}