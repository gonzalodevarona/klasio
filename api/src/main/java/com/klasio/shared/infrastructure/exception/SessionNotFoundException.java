package com.klasio.shared.infrastructure.exception;

import java.time.LocalDate;
import java.util.UUID;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(UUID classId, LocalDate sessionDate) {
        super("No session found for class " + classId + " on " + sessionDate);
    }

    public SessionNotFoundException(UUID sessionId) {
        super("Session not found: " + sessionId);
    }
}
