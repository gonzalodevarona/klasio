package com.klasio.auth.domain.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("This email address is already registered in this league");
    }
}
