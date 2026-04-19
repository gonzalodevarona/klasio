package com.klasio.shared.infrastructure.exception;

public class CancellationWindowExpiredException extends RuntimeException {
    public CancellationWindowExpiredException(int cutoffMinutes) {
        super("Cancellation is not allowed within " + cutoffMinutes
                + " minute(s) of the session start time.");
    }
}
