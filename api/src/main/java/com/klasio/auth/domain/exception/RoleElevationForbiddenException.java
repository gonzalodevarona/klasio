package com.klasio.auth.domain.exception;

public class RoleElevationForbiddenException extends RuntimeException {
    public RoleElevationForbiddenException(String message) {
        super(message);
    }
}
