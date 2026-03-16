package com.klasio.shared.infrastructure.exception;

public class TenantAlreadyInactiveException extends RuntimeException {

    public TenantAlreadyInactiveException(String message) {
        super(message);
    }
}
