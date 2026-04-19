package com.klasio.shared.infrastructure.exception;

import java.util.UUID;

public class RegistrationNotFoundException extends RuntimeException {
    public RegistrationNotFoundException(UUID registrationId) {
        super("Attendance registration not found: " + registrationId);
    }
}
