package com.careflow.serviceops.exception;

public class StaleVersionException extends RuntimeException {
    private final long currentVersion;

    public StaleVersionException(long expectedVersion, long currentVersion) {
        super("This work order was changed by someone else (you had version " + expectedVersion
                + ", current is " + currentVersion + "). Refresh and try again.");
        this.currentVersion = currentVersion;
    }

    public long getCurrentVersion() { return currentVersion; }
}