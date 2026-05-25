package com.klasio.shared.infrastructure.exception;

public class DropInAlreadyRegisteredException extends RuntimeException {
    public DropInAlreadyRegisteredException(String message) { super(message); }
}
