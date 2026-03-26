package com.klasio.shared.infrastructure.exception;

public class ClassAlreadyInactiveException extends RuntimeException {

    public ClassAlreadyInactiveException(String message) {
        super(message);
    }
}
