package com.careflow.serviceops.api;

import com.careflow.serviceops.exception.PreconditionRequiredException;

final class IfMatchParser {
    private IfMatchParser() { }

    /** Accepts 3, "3" or W/"3". Missing header -> 428, garbage -> 400. */
    static long parse(String header) {
        if (header == null || header.isBlank()) {
            throw new PreconditionRequiredException(
                    "An If-Match header containing the work order version is required.");
        }
        String value = header.trim();
        if (value.startsWith("W/")) value = value.substring(2);
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            long version = Long.parseLong(value);
            if (version < 0) throw new NumberFormatException();
            return version;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("If-Match must be a work order version such as \"3\".");
        }
    }
}