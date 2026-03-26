package com.klasio.shared.infrastructure.exception;

public class ClassAlreadyActiveException extends RuntimeException {

    public ClassAlreadyActiveException(String message) {
        super(message);
    }
}
